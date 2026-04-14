import math
import os
import struct
import wave

SAMPLE_RATE = 44100


def envelope(index, total, attack=0.02, release=0.25):
    position = index / max(total - 1, 1)
    if position < attack:
        return position / max(attack, 1e-6)
    if position > 1 - release:
        return max((1 - position) / max(release, 1e-6), 0)
    return 1.0


def write_wave(path, samples):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with wave.open(path, "wb") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(SAMPLE_RATE)
        frames = bytearray()
        for sample in samples:
            clamped = max(-1.0, min(1.0, sample))
            frames.extend(struct.pack("<h", int(clamped * 32767)))
        wav_file.writeframes(bytes(frames))


def synth_click(duration=0.18):
    total = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(total):
        t = i / SAMPLE_RATE
        env = envelope(i, total, attack=0.01, release=0.75)
        tone = (
            0.55 * math.sin(2 * math.pi * 880 * t)
            + 0.25 * math.sin(2 * math.pi * 1320 * t)
            + 0.12 * math.sin(2 * math.pi * 1760 * t)
        )
        sparkle = 0.08 * math.sin(2 * math.pi * 2200 * t) * math.exp(-14 * t)
        samples.append((tone * math.exp(-8 * t) + sparkle) * env * 0.9)
    return samples


def synth_note(freq, duration, volume=0.4):
    total = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(total):
        t = i / SAMPLE_RATE
        env = envelope(i, total, attack=0.04, release=0.35)
        wave_sum = (
            math.sin(2 * math.pi * freq * t)
            + 0.35 * math.sin(2 * math.pi * freq * 2 * t)
            + 0.18 * math.sin(2 * math.pi * freq * 3 * t)
        ) / 1.53
        wobble = 0.015 * math.sin(2 * math.pi * 5 * t)
        samples.append((wave_sum + wobble) * env * volume)
    return samples


def synth_silly_loop():
    melody = [
        (523.25, 0.33), (659.25, 0.33), (783.99, 0.33), (659.25, 0.33),
        (698.46, 0.33), (587.33, 0.33), (523.25, 0.66), (392.00, 0.33),
        (523.25, 0.33), (659.25, 0.33), (783.99, 0.33), (880.00, 0.66),
        (698.46, 0.33), (659.25, 0.33), (587.33, 0.33), (523.25, 0.66),
    ]
    bass = [261.63, 261.63, 196.00, 196.00, 220.00, 220.00, 196.00, 196.00]

    samples = []
    bass_index = 0
    bass_note = []
    bass_pos = 0

    for freq, duration in melody:
        lead = synth_note(freq, duration, volume=0.26)
        if bass_pos >= len(bass_note):
            bass_freq = bass[bass_index % len(bass)]
            bass_note = synth_note(bass_freq, 0.66, volume=0.12)
            bass_index += 1
            bass_pos = 0

        for lead_sample in lead:
            bass_sample = bass_note[bass_pos] if bass_pos < len(bass_note) else 0.0
            percussion = 0.03 * math.sin(2 * math.pi * 60 * (len(samples) / SAMPLE_RATE))
            samples.append(lead_sample + bass_sample + percussion)
            bass_pos += 1

    fade = int(SAMPLE_RATE * 0.5)
    for i in range(1, min(fade, len(samples)) + 1):
        samples[-i] *= i / fade

    return samples


def main():
    project_root = "/home/ubuntu/Sudoku"
    raw_dir = os.path.join(project_root, "app", "src", "main", "res", "raw")
    write_wave(os.path.join(raw_dir, "boop_click.wav"), synth_click())
    write_wave(os.path.join(raw_dir, "silly_background_music.wav"), synth_silly_loop())


if __name__ == "__main__":
    main()
