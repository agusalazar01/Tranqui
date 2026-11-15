#!/usr/bin/env bash
# Prepara una grabación para Tranqui: recorta, iguala el volumen, empalma el final con el
# principio para que el bucle no tenga corte y la guarda como Opus (.ogg), liviano.
#
# Uso: tools/preparar_sonido.sh entrada salida.ogg [segundos_max] [modo]
#   modo = bucle (por defecto) | golpe (un golpe de cuenco + silencio, sin empalme)
set -euo pipefail

IN="$1"
OUT="$2"
MAX="${3:-120}"
MODO="${4:-bucle}"
F=4 # segundos de empalme

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# 1) Recorte, estéreo 48 kHz y volumen parejo (-24 LUFS: suave, para fondo).
ffmpeg -nostdin -v error -y -i "$IN" -t "$MAX" -ac 2 -ar 48000 \
  -af "loudnorm=I=-24:TP=-3:LRA=11" "$TMP/norm.wav"

if [ "$MODO" = "golpe" ]; then
  ffmpeg -nostdin -v error -y -i "$TMP/norm.wav" -af "apad=pad_dur=8" "$TMP/final.wav"
else
  D=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$TMP/norm.wav")
  E=$(awk -v d="$D" -v f="$F" 'BEGIN{printf "%.3f", d-f}')
  # 2) Empalme: el final se funde con el principio, así el salto del bucle no se escucha.
  W="-c:a pcm_s16le -ar 48000 -ac 2"
  ffmpeg -nostdin -v error -y -i "$TMP/norm.wav" -af "atrim=0:$F,asetpts=PTS-STARTPTS,afade=t=in:d=$F:curve=qsin" $W "$TMP/head.wav"
  ffmpeg -nostdin -v error -y -i "$TMP/norm.wav" -af "atrim=$E:$D,asetpts=PTS-STARTPTS,afade=t=out:d=$F:curve=qsin" $W "$TMP/tail.wav"
  ffmpeg -nostdin -v error -y -i "$TMP/norm.wav" -af "atrim=$F:$E,asetpts=PTS-STARTPTS" $W "$TMP/mid.wav"
  ffmpeg -nostdin -v error -y -i "$TMP/head.wav" -i "$TMP/tail.wav" -filter_complex "amix=inputs=2:normalize=0:duration=shortest" $W "$TMP/x.wav"
  printf "file '%s'\nfile '%s'\n" "$TMP/x.wav" "$TMP/mid.wav" > "$TMP/lista.txt"
  ffmpeg -nostdin -v error -y -f concat -safe 0 -i "$TMP/lista.txt" $W "$TMP/final.wav"
fi

# 3) Opus 64 kbps: buena calidad para ambientes, unas 4 veces más liviano que el MP3 original.
ffmpeg -nostdin -v error -y -i "$TMP/final.wav" -c:a libopus -b:a 64k -vbr on "$OUT"
echo "listo: $OUT ($(du -h "$OUT" | cut -f1))"
