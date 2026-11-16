"""
Descarga y prepara los sonidos de la biblioteca que no están en el repositorio.

Lee app/src/main/assets/soundscape/catalogo.json y, para cada sonido que no es «local»:
  - bigsoundbank: prueba primero los formatos sin pérdida (FLAC, WAV) y si no, MP3.
  - incompetech: la versión MP3 de 320 kbps de Kevin MacLeod.
  - freesound: la vista previa de mejor calidad de la página pública del sonido (sin cuenta).
  - freesound_ritmo: golpes sueltos de tambor (grabaciones reales) acomodados en un ritmo parejo,
    con pequeñas variaciones de tiempo y fuerza para que no suene a máquina.
Después lo pasa por tools/preparar_sonido.sh. Si una descarga falla, ese sonido no aparece
en la app (la página solo muestra los archivos que existen) y la compilación sigue.

Se corre en GitHub Actions: python tools/preparar_biblioteca.py
"""
import json
import os
import random
import re
import subprocess
import sys
import tempfile
import urllib.parse
import urllib.request

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOGO = os.path.join(RAIZ, "app/src/main/assets/soundscape/catalogo.json")
SALIDA = os.path.join(RAIZ, "app/src/main/assets/soundscape/sounds")
SCRIPT = os.path.join(RAIZ, "tools/preparar_sonido.sh")
UA = {"User-Agent": "Tranqui/1.0 (GitHub Actions; app de bienestar sin fines de lucro)"}


def urls(fuente):
    if fuente["type"] == "bigsoundbank":
        n = fuente["id"]
        return [f"https://bigsoundbank.com/UPLOAD/{ext}/{n}.{ext}" for ext in ("flac", "wav")] + [
            f"https://bigsoundbank.com/UPLOAD/mp3/{n}.mp3"]
    if fuente["type"] == "incompetech":
        t = urllib.parse.quote(fuente["title"])
        return [f"https://incompetech.com/music/royalty-free/mp3-royaltyfree/{t}.mp3"]
    return []


def freesound_preview(n):
    """La página pública del sonido trae los enlaces a las vistas previas (HQ ogg o mp3)."""
    req = urllib.request.Request(f"https://freesound.org/s/{n}/", headers=UA)
    html = urllib.request.urlopen(req, timeout=60).read().decode("utf-8", "replace")
    enlaces = re.findall(r"https://cdn\.freesound\.org/previews/[^\"'\s]+?-hq\.(?:ogg|mp3)", html)
    if not enlaces:
        raise IOError("sin vista previa en la página")
    # Primero ogg (mejor calidad a igual tamaño).
    return sorted(set(enlaces), key=lambda u: 0 if u.endswith(".ogg") else 1)


def decodificar(archivo, sr=48000):
    import numpy as np
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", archivo, "-ac", "1", "-ar", str(sr), "-f", "f32le", "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype=np.float32).copy()


def armar_ritmo(fuente, destino_wav, tmp, sr=48000):
    """Ritmo parejo con golpes reales; cada golpe varía un poco en tiempo (±6 ms) y fuerza."""
    import numpy as np
    golpes = []
    for n in fuente["ids"]:
        for url in freesound_preview(n):
            try:
                a = os.path.join(tmp, f"g{n}." + url.rsplit(".", 1)[-1])
                bajar(url, a, minimo=2_000)
                g = decodificar(a, sr)
                g = g[: int(sr * 1.2)]
                golpes.append(g / (np.abs(g).max() or 1))
                break
            except Exception as e:
                print(f"..  golpe {n}: {e}")
    if not golpes:
        raise IOError("no se pudo bajar ningún golpe")
    paso = 60.0 / fuente["bpm"]
    total = int(sr * fuente["seconds"])
    out = np.zeros(total + sr * 2, dtype=np.float32)
    rnd = random.Random(7)
    t, i = 0.0, 0
    while t < fuente["seconds"]:
        g = golpes[i % len(golpes)] if rnd.random() < 0.8 else rnd.choice(golpes)
        fuerza = (1.0 if i % 4 == 0 else 0.78) * rnd.uniform(0.9, 1.0)
        ini = int((t + rnd.uniform(-0.006, 0.006)) * sr)
        if ini >= 0:
            out[ini:ini + len(g)] += g[: max(0, len(out) - ini)] * fuerza
        t += paso
        i += 1
    out = out[:total]
    out /= (np.abs(out).max() or 1) / 0.8
    raw = os.path.join(tmp, "ritmo.f32")
    out.tofile(raw)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "f32le", "-ar", str(sr), "-ac", "1", "-i", raw,
                    "-af", "aecho=0.6:0.5:60:0.25", "-ac", "2", destino_wav], check=True)


def bajar(url, destino, minimo=20_000):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=120) as r, open(destino, "wb") as f:
        if r.status != 200:
            raise IOError(r.status)
        f.write(r.read())
    if os.path.getsize(destino) < minimo:
        raise IOError("archivo demasiado chico")


def main():
    cat = json.load(open(CATALOGO, encoding="utf-8"))
    ok, fallas = 0, []
    for s in cat["sounds"]:
        if s["source"]["type"] == "local":
            continue
        destino = os.path.join(SALIDA, s["id"] + ".ogg")
        if os.path.exists(destino):
            continue
        hecho = False
        with tempfile.TemporaryDirectory() as tmp:
            if s["source"]["type"] == "freesound_ritmo":
                try:
                    wav = os.path.join(tmp, "ritmo.wav")
                    armar_ritmo(s["source"], wav, tmp)
                    subprocess.run([SCRIPT, wav, destino, str(s.get("max", 150)), s.get("mode", "bucle"),
                                    str(s.get("lufs", -23))], check=True)
                    print(f"OK  {s['id']:18s} ← ritmo armado")
                    ok += 1
                except Exception as e:
                    print(f"..  {s['id']:18s} ritmo: {e}")
                    fallas.append(s["id"])
                continue
            fuentes = urls(s["source"])
            if s["source"]["type"] == "freesound":
                try:
                    fuentes = freesound_preview(s["source"]["id"])
                except Exception as e:
                    print(f"..  {s['id']:18s} freesound: {e}")
                    fuentes = []
            for url in fuentes:
                ext = url.rsplit(".", 1)[-1]
                archivo = os.path.join(tmp, "in." + ext)
                try:
                    bajar(url, archivo)
                    subprocess.run([SCRIPT, archivo, destino, str(s.get("max", 150)), s.get("mode", "bucle"),
                                    str(s.get("lufs", -23))], check=True)
                    print(f"OK  {s['id']:18s} ← {url}")
                    hecho = True
                    break
                except Exception as e:  # probar el siguiente formato
                    print(f"..  {s['id']:18s} {url}: {e}")
        if hecho:
            ok += 1
        else:
            fallas.append(s["id"])
    print(f"\n{ok} sonidos descargados y preparados.")
    if fallas:
        print("::warning::No se pudieron preparar: " + ", ".join(fallas))


if __name__ == "__main__":
    main()
    sys.exit(0)
