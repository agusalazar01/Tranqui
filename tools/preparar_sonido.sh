#!/usr/bin/env bash
# Prepara una grabación para Tranqui, cuidando la calidad para escuchar con auriculares:
#  1. Recorta (si hace falta) y pasa a 48 kHz estéreo con precisión de 32 bits.
#  2. Iguala el volumen en DOS pasadas (loudnorm lineal): mide y aplica una sola ganancia
#     fija, sin «bombeo» ni compresión que cambie el sonido.
#  3. Empalma el final con el principio (fundido de 6 s de igual potencia) para que el
#     bucle no tenga corte. En «golpe» (cuencos) no empalma: agrega silencio al final.
#  4. Codifica en Opus 96 kbps estéreo: para ambientes es prácticamente indistinguible del
#     original y pesa una fracción.
#
# Uso: tools/preparar_sonido.sh entrada salida.ogg [segundos_max] [modo] [lufs]
#   modo = bucle (por defecto) | golpe
set -euo pipefail

IN="$1"
OUT="$2"
MAX="${3:-150}"
MODO="${4:-bucle}"
LUFS="${5:--23}"
F=6

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
W="-c:a pcm_f32le -ar 48000 -ac 2"

# 1) Recorte y formato de trabajo (sin pérdida).
ffmpeg -nostdin -v error -y -i "$IN" -t "$MAX" -af "highpass=f=18" $W "$TMP/base.wav"

# 2) Volumen: primera pasada mide, segunda aplica una ganancia lineal.
MED=$(ffmpeg -nostdin -hide_banner -i "$TMP/base.wav" \
  -af "loudnorm=I=$LUFS:TP=-2:LRA=20:print_format=json" -f null - 2>&1 | \
  python3 -c 'import sys,json,re; t=sys.stdin.read(); print(json.dumps(json.loads(re.findall(r"\{[^{}]*\}", t)[-1])))')
read -r MI MTP MLRA MTH MOFF < <(python3 -c '
import json,sys; d=json.loads(sys.argv[1])
print(d["input_i"], d["input_tp"], d["input_lra"], d["input_thresh"], d["target_offset"])' "$MED")
ffmpeg -nostdin -v error -y -i "$TMP/base.wav" \
  -af "loudnorm=I=$LUFS:TP=-2:LRA=20:measured_I=$MI:measured_TP=$MTP:measured_LRA=$MLRA:measured_thresh=$MTH:offset=$MOFF:linear=true,aresample=48000" \
  $W "$TMP/norm.wav"

if [ "$MODO" = "golpe" ]; then
  ffmpeg -nostdin -v error -y -i "$TMP/norm.wav" -af "apad=pad_dur=8" $W "$TMP/final.wav"
else
  D=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$TMP/norm.wav")
  # Grabaciones cortas: el empalme ocupa como mucho un cuarto del total.
  F=$(awk -v d="$D" -v f="$F" 'BEGIN{x=d/4; if (x<f) printf "%.3f", x; else print f}')
  E=$(awk -v d="$D" -v f="$F" 'BEGIN{printf "%.3f", d-f}')
  # 3) Empalme: el final se funde con el principio, así el salto del bucle no se escucha.
  ffmpeg -nostdin -v error -y -i "$TMP/norm.wav" -af "atrim=0:$F,asetpts=PTS-STARTPTS,afade=t=in:d=$F:curve=qsin" $W "$TMP/head.wav"
  ffmpeg -nostdin -v error -y -i "$TMP/norm.wav" -af "atrim=$E:$D,asetpts=PTS-STARTPTS,afade=t=out:d=$F:curve=qsin" $W "$TMP/tail.wav"
  ffmpeg -nostdin -v error -y -i "$TMP/norm.wav" -af "atrim=$F:$E,asetpts=PTS-STARTPTS" $W "$TMP/mid.wav"
  ffmpeg -nostdin -v error -y -i "$TMP/head.wav" -i "$TMP/tail.wav" -filter_complex "amix=inputs=2:normalize=0:duration=shortest" $W "$TMP/x.wav"
  printf "file '%s'\nfile '%s'\n" "$TMP/x.wav" "$TMP/mid.wav" > "$TMP/lista.txt"
  ffmpeg -nostdin -v error -y -f concat -safe 0 -i "$TMP/lista.txt" $W "$TMP/final.wav"
fi

# 4) Opus 96 kbps estéreo, máxima complejidad del codificador.
ffmpeg -nostdin -v error -y -i "$TMP/final.wav" -c:a libopus -b:a 96k -vbr on \
  -compression_level 10 -frame_duration 20 "$OUT"
echo "listo: $OUT ($(du -h "$OUT" | cut -f1))"
