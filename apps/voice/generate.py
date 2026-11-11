"""Voices the spirit box: every trait word and every answer, in each language, read by Piper and broken up into a
dead radio's voice. Writes assets/restless/sounds/spirit_box and the spirit_box.* entries of sounds.json; run again
after a word or an answer is added to the lang files."""

import json
import shutil
import urllib.request
import zlib
from pathlib import Path

import numpy as np
import soundfile
from piper import PiperVoice, SynthesisConfig
from scipy.linalg import solve_toeplitz
from scipy.signal import butter, fftconvolve, get_window, lfilter, resample_poly, sosfilt

ROOT = Path(__file__).resolve().parent
ASSETS = ROOT.parent / "mod/core/src/main/resources/assets/restless"
OUT = ASSETS / "sounds/spirit_box"
VOICES_DIR = ROOT / "voices"
VOICE_URL = "https://huggingface.co/rhasspy/piper-voices/resolve/main/"

# Both trained on CC0 recordings (OHF-Voice/voice-datasets), so the clips can ship with the mod.
VOICES = {
    "en_us": "en/en_US/joe/medium/en_US-joe-medium",
    "ru_ru": "ru/ru_RU/dmitri/medium/ru_RU-dmitri-medium",
}
# Each clip is made this many times, each broken differently; the game picks one at random.
VARIANTS = 2
HISSES = 4


def fetch(voice: str) -> Path:
    VOICES_DIR.mkdir(exist_ok=True)
    model = VOICES_DIR / (voice.rsplit("/", 1)[1] + ".onnx")
    for path, url in ((model, VOICE_URL + voice + ".onnx"), (Path(str(model) + ".json"), VOICE_URL + voice + ".onnx.json")):
        if not path.exists():
            print("downloading", url)
            urllib.request.urlretrieve(url, path)
    return model


def clips(lang: str) -> dict[str, str]:
    """What the box says in a language: word.<id> for each trait word, answer.<id> for each answer."""
    texts = json.loads((ASSETS / f"lang/{lang}.json").read_text(encoding="utf-8"))
    out = {}
    for key, text in texts.items():
        if key.startswith("word.restless."):
            out["word." + key.removeprefix("word.restless.")] = text
        elif key.startswith("spirit_box.restless.") and key != "spirit_box.restless.hiss":
            out["answer." + key.removeprefix("spirit_box.restless.")] = text
    return out


def speak(voice: PiperVoice, text: str, rng: np.random.Generator) -> np.ndarray:
    config = SynthesisConfig(
        length_scale=rng.uniform(1.15, 1.4), noise_scale=rng.uniform(0.6, 0.9), noise_w_scale=rng.uniform(0.7, 1.0)
    )
    audio = np.concatenate([chunk.audio_float_array for chunk in voice.synthesize(text, syn_config=config)])
    loud = np.flatnonzero(np.abs(audio) > 0.02)
    return audio[loud[0] : loud[-1] + 1] if loud.size else audio


def band(audio: np.ndarray, rate: int, low: float, high: float) -> np.ndarray:
    return sosfilt(butter(4, [low, high], btype="band", fs=rate, output="sos"), audio)


def reverb(audio: np.ndarray, rate: int, rng: np.random.Generator, seconds: float) -> np.ndarray:
    t = np.arange(int(rate * seconds)) / rate
    tail = rng.standard_normal(t.size) * np.exp(-t / (seconds / 4))
    wet = fftconvolve(audio, tail)
    return wet / (np.max(np.abs(wet)) + 1e-9)


def static(length: int, rate: int, rng: np.random.Generator, level: float) -> np.ndarray:
    """Radio static: hiss that swells and fades, with crackles."""
    noise = band(rng.standard_normal(length), rate, 500, 6000)
    noise /= np.max(np.abs(noise)) + 1e-9
    swell = np.interp(np.arange(length), np.linspace(0, length, 8), rng.uniform(0.3, 1.0, 8))
    crackle = np.zeros(length)
    crackle[rng.integers(0, length, max(1, length * 25 // rate))] = rng.uniform(-1, 1, max(1, length * 25 // rate))
    return level * noise * swell + 0.5 * level * band(crackle, rate, 2000, 9000) * 6


def whisper(audio: np.ndarray, rate: int, rng: np.random.Generator) -> np.ndarray:
    """The words with the voice taken out: each moment's mouth shape kept (linear prediction), the throat's buzz under
    it replaced by breath, so no living speaker is left in it."""
    hop = int(rate * 0.0125)
    window = get_window("hann", 2 * hop)
    order = 2 + rate // 1000
    tilt = lfilter([1, -0.9], [1], audio)
    out = np.zeros(audio.size + 2 * hop)
    for at in range(0, audio.size, hop):
        frame = np.zeros(2 * hop)
        part = tilt[at : at + 2 * hop]
        frame[: part.size] = part
        frame *= window
        r = np.correlate(frame, frame, "full")[frame.size - 1 : frame.size + order]
        if r[0] < 1e-8:
            continue
        r[0] *= 1.001
        shape = np.concatenate([[1], -solve_toeplitz(r[:order], r[1:])])
        breath = lfilter([1], shape, rng.standard_normal(frame.size)) * window
        out[at : at + frame.size] += breath * np.sqrt(np.sum(frame**2) / (np.sum(breath**2) + 1e-12))
    return lfilter([1], [1, -0.9], out)


def ghost(audio: np.ndarray, rate: int, rng: np.random.Generator) -> np.ndarray:
    """The voice as the box gives it back: whispered, lower and slower, through a narrow band, crushed, cut out in
    places, under static and with an echo; now and then the echo swells in before the word, as if played backwards."""
    audio = whisper(audio, rate, rng)
    slow = rng.uniform(0.82, 0.92)
    audio = resample_poly(audio, 1000, int(1000 * slow))
    # A whisper is carried by its higher formants and hiss, so the band is wider than a radio voice's.
    audio = band(audio, rate, 250, 4500)
    drive = rng.uniform(1.2, 2)
    audio = np.tanh(audio / (np.max(np.abs(audio)) + 1e-9) * drive) / np.tanh(drive)
    steps = 2 ** int(rng.integers(7, 9))
    audio = np.round(audio * steps) / steps
    for _ in range(int(rng.integers(0, 3))):
        at, gap = int(rng.integers(0, audio.size)), int(rate * rng.uniform(0.015, 0.05))
        audio[at : at + gap] *= 0.1
    pad = int(rate * 0.15)
    if rng.random() < 0.4:
        swell = reverb(audio[::-1], rate, rng, 0.6)[::-1]
        audio = 0.35 * swell + np.concatenate([np.zeros(swell.size - audio.size), audio])
    else:
        wet = reverb(audio, rate, rng, 0.6)
        audio = np.concatenate([audio, np.zeros(wet.size - audio.size)]) + 0.15 * wet
    audio = np.concatenate([np.zeros(pad), audio, np.zeros(pad)])
    audio += static(audio.size, rate, rng, rng.uniform(0.02, 0.05))
    return finish(audio, rate)


def hiss(rate: int, rng: np.random.Generator) -> np.ndarray:
    return finish(static(int(rate * rng.uniform(0.5, 0.9)), rate, rng, 0.5), rate)


def finish(audio: np.ndarray, rate: int) -> np.ndarray:
    fade = int(rate * 0.01)
    ramp = np.linspace(0, 1, fade)
    audio[:fade] *= ramp
    audio[-fade:] *= ramp[::-1]
    return (0.85 * audio / (np.max(np.abs(audio)) + 1e-9)).astype(np.float32)


def seed(*parts: str) -> np.random.Generator:
    return np.random.default_rng(zlib.crc32(".".join(parts).encode()))


def write(audio: np.ndarray, rate: int, name: str) -> str:
    path = OUT / (name + ".ogg")
    path.parent.mkdir(parents=True, exist_ok=True)
    soundfile.write(path, audio, rate, format="OGG", subtype="VORBIS")
    return "restless:spirit_box/" + name


def main() -> None:
    shutil.rmtree(OUT, ignore_errors=True)
    events = {}
    rate = 22050
    events["spirit_box.hiss"] = [write(hiss(rate, seed("hiss", str(n))), rate, f"hiss_{n}") for n in range(HISSES)]
    for lang, name in VOICES.items():
        voice = PiperVoice.load(fetch(name))
        rate = voice.config.sample_rate
        for key, text in clips(lang).items():
            sounds = []
            for n in range(VARIANTS):
                rng = seed(lang, key, str(n))
                sounds.append(write(ghost(speak(voice, text, rng), rate, rng), rate, f"{lang}/{key.replace('.', '/')}_{n}"))
            events[f"spirit_box.{lang}.{key}"] = sounds
        print(lang, len(events), "events so far")

    path = ASSETS / "sounds.json"
    table = {k: v for k, v in json.loads(path.read_text(encoding="utf-8")).items() if not k.startswith("spirit_box.")}
    for event, sounds in events.items():
        table[event] = {"subtitle": "subtitles.restless.spirit_box", "sounds": [{"name": s} for s in sounds]}
    path.write_text(json.dumps(table, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    size = sum(f.stat().st_size for f in OUT.rglob("*.ogg"))
    print(len(events), "events,", sum(1 for _ in OUT.rglob("*.ogg")), "clips,", size // 1024, "KiB")


if __name__ == "__main__":
    main()
