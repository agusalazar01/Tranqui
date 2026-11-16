"""
Genera la voz de las prácticas guiadas con Kokoro-82M (licencia Apache 2.0).

Lee las frases directamente de Content.kt (pasos de meditación y relajación, lo que dice
cada fase de respiración y las frases fijas VOZ_*), así nunca quedan desfasadas.
Cada frase se guarda como app/src/main/assets/voz/<voz>/<clave>.ogg, donde <clave> son
los primeros 12 caracteres del SHA-1 del texto (la app calcula la misma clave).

Se corre en GitHub Actions (necesita internet para bajar el modelo y espeak-ng instalado):
    python tools/generar_voz.py
"""
import hashlib
import os
import re
import subprocess
import sys
import tempfile

import numpy as np
import soundfile as sf

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONTENT = os.path.join(RAIZ, "app/src/main/java/com/tranqui/app/Content.kt")
SALIDA = os.path.join(RAIZ, "app/src/main/assets/voz")

# Voces en español de Kokoro. «speed» < 1 habla más lento y tranquilo.
VOCES = {"dora": "ef_dora", "alex": "em_alex"}
VELOCIDAD = 0.82


def frases():
    s = open(CONTENT, encoding="utf-8").read()
    textos = []
    textos += re.findall(r'const val VOZ_\w+ = "([^"]*)"', s)
    for m in re.finditer(r'Phase\("([^"]*)",\s*[\d.]+f,\s*[\d.]+f(?:,\s*(null|"([^"]*)"))?\)', s):
        if m.group(2) == "null":
            continue
        textos.append(m.group(3) if m.group(3) is not None else m.group(1))
    for m in re.finditer(r"steps = listOf\((.*?)\n        \)", s, re.S):
        textos += re.findall(r'"([^"]*)"', m.group(1))
    partes = []
    for t in textos:
        for p in t.split("…"):
            p = p.strip()
            if p and p not in partes:
                partes.append(p)
    return partes


def clave(texto):
    return hashlib.sha1(texto.strip().encode("utf-8")).hexdigest()[:12]


def main():
    from kokoro import KPipeline

    lista = frases()
    print(f"{len(lista)} frases")
    pipe = KPipeline(lang_code="e")  # español
    for nombre, voz in VOCES.items():
        carpeta = os.path.join(SALIDA, nombre)
        os.makedirs(carpeta, exist_ok=True)
        for texto in lista:
            # Las frases que empiezan con «y» o minúscula se leen igual de bien sueltas.
            trozos = [a for _, _, a in pipe(texto, voice=voz, speed=VELOCIDAD)]
            if not trozos:
                print("sin audio:", texto, file=sys.stderr)
                continue
            audio = np.concatenate([np.asarray(t, dtype=np.float32) for t in trozos])
            with tempfile.TemporaryDirectory() as tmp:
                wav = os.path.join(tmp, "v.wav")
                sf.write(wav, audio, 24000)
                destino = os.path.join(carpeta, clave(texto) + ".ogg")
                # Voz cálida: sin graves molestos ni agudos filosos, volumen parejo,
                # un instante de silencio al principio y al final para que no entre de golpe.
                subprocess.run([
                    "ffmpeg", "-nostdin", "-v", "error", "-y", "-i", wav,
                    "-af", "highpass=f=70,lowpass=f=7500,loudnorm=I=-20:TP=-3,"
                           "adelay=250,apad=pad_dur=0.4,afade=t=in:d=0.08",
                    "-ac", "1", "-c:a", "libopus", "-b:a", "40k", destino,
                ], check=True)
            print(nombre, clave(texto), texto)


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--listar":
        for f in frases():
            print(clave(f), f)
    else:
        main()
