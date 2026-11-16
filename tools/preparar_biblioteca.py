"""
Descarga y prepara los sonidos de la biblioteca que no están en el repositorio.

Lee app/src/main/assets/soundscape/catalogo.json y, para cada sonido que no es «local»:
  - bigsoundbank: prueba primero los formatos sin pérdida (FLAC, WAV) y si no, MP3.
  - incompetech: la versión MP3 de 320 kbps de Kevin MacLeod.
Después lo pasa por tools/preparar_sonido.sh. Si una descarga falla, ese sonido no aparece
en la app (la página solo muestra los archivos que existen) y la compilación sigue.

Se corre en GitHub Actions: python tools/preparar_biblioteca.py
"""
import json
import os
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


def bajar(url, destino):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=120) as r, open(destino, "wb") as f:
        if r.status != 200:
            raise IOError(r.status)
        f.write(r.read())
    if os.path.getsize(destino) < 20_000:
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
            for url in urls(s["source"]):
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
