"""
Incluye en la app las voces grabadas con «Grabar mi voz».

Cada archivo voces/<nombre>.zip (exportado desde la app) se descomprime, se limpia y queda
como app/src/main/assets/voz/<nombre>/<clave>.ogg. En la app aparece como «Voz de <Nombre>».

Limpieza de cada frase (pensada para una grabación con el celular):
  - corta el silencio del principio y del final (y el ruido del botón),
  - saca graves molestos (golpes, viento) y un poco de ruido de fondo constante,
  - iguala el volumen de todas las frases,
  - agrega un instante de silencio para que la voz no entre de golpe.

Se corre en GitHub Actions: python tools/preparar_voces.py
"""
import glob
import os
import re
import subprocess
import tempfile
import zipfile

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENTRADA = os.path.join(RAIZ, "voces")
SALIDA = os.path.join(RAIZ, "app/src/main/assets/voz")

FILTRO = ",".join([
    "highpass=f=80",
    "afftdn=nr=10:nf=-45",
    # recorta silencio al principio y al final (dos pasadas con areverse)
    "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.15",
    "areverse",
    "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.3",
    "areverse",
    "loudnorm=I=-20:TP=-3:LRA=9",
    "adelay=250",
    "apad=pad_dur=0.4",
    "afade=t=in:d=0.05",
])


def main():
    for z in sorted(glob.glob(os.path.join(ENTRADA, "*.zip"))):
        nombre = re.sub(r"[^a-z0-9]", "", os.path.splitext(os.path.basename(z))[0].lower()) or "propia"
        if nombre in ("dora", "alex", "mia"):
            nombre += "2"
        carpeta = os.path.join(SALIDA, nombre)
        os.makedirs(carpeta, exist_ok=True)
        n = 0
        with tempfile.TemporaryDirectory() as tmp:
            zipfile.ZipFile(z).extractall(tmp)
            for f in glob.glob(os.path.join(tmp, "**", "*.m4a"), recursive=True):
                clave = os.path.splitext(os.path.basename(f))[0]
                if not re.fullmatch(r"[0-9a-f]{12}", clave):
                    continue
                subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-y", "-i", f, "-af", FILTRO,
                                "-ac", "1", "-ar", "48000", "-c:a", "libopus", "-b:a", "64k",
                                os.path.join(carpeta, clave + ".ogg")], check=True)
                n += 1
        print(f"voz «{nombre}»: {n} frases")


if __name__ == "__main__":
    main()
