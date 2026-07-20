import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { inflateSync } from "node:zlib";

const extensionRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const manifest = JSON.parse(await readFile(path.join(extensionRoot, "package.json"), "utf8"));
const iconPath = path.join(extensionRoot, manifest.icon);
const png = await readFile(iconPath);

const { width, height, alphaValues } = decodeRgbaPng(png);
if (width !== 256 || height !== 256) {
  throw new Error(`Extension icon must be 256x256, received ${width}x${height}`);
}

const cornerIndexes = [0, width - 1, (height - 1) * width, width * height - 1];
if (cornerIndexes.some((index) => alphaValues[index] !== 0)) {
  throw new Error("Extension icon corners must be transparent");
}
if (!alphaValues.some((alpha) => alpha === 0)) {
  throw new Error("Extension icon must contain transparent pixels");
}
if (!alphaValues.some((alpha) => alpha === 1)) {
  throw new Error("Extension icon must contain fully opaque artwork");
}

process.stdout.write(`Validated transparent extension icon ${manifest.icon}\n`);

function decodeRgbaPng(buffer) {
  const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  if (!buffer.subarray(0, signature.length).equals(signature)) {
    throw new Error("Extension icon must be a PNG file");
  }

  let offset = signature.length;
  let header;
  const imageChunks = [];
  while (offset < buffer.length) {
    const length = buffer.readUInt32BE(offset);
    const type = buffer.toString("ascii", offset + 4, offset + 8);
    const data = buffer.subarray(offset + 8, offset + 8 + length);
    offset += length + 12;
    if (type === "IHDR") {
      header = {
        width: data.readUInt32BE(0),
        height: data.readUInt32BE(4),
        bitDepth: data[8],
        colorType: data[9],
      };
    } else if (type === "IDAT") {
      imageChunks.push(data);
    } else if (type === "IEND") {
      break;
    }
  }

  if (header === undefined || imageChunks.length === 0) {
    throw new Error("Extension icon PNG is missing image data");
  }
  if (header.colorType !== 6 || ![8, 16].includes(header.bitDepth)) {
    throw new Error("Extension icon must use 8-bit or 16-bit RGBA color");
  }

  const sampleBytes = header.bitDepth / 8;
  const bytesPerPixel = 4 * sampleBytes;
  const rowBytes = header.width * bytesPerPixel;
  const compressed = Buffer.concat(imageChunks);
  const filtered = inflateSync(compressed);
  const expectedLength = header.height * (rowBytes + 1);
  if (filtered.length !== expectedLength) {
    throw new Error("Extension icon PNG has an unsupported scanline layout");
  }

  const pixels = Buffer.alloc(header.height * rowBytes);
  for (let row = 0; row < header.height; row += 1) {
    const filteredOffset = row * (rowBytes + 1);
    const filter = filtered[filteredOffset];
    const source = filtered.subarray(filteredOffset + 1, filteredOffset + 1 + rowBytes);
    const targetOffset = row * rowBytes;
    for (let column = 0; column < rowBytes; column += 1) {
      const left = column >= bytesPerPixel ? pixels[targetOffset + column - bytesPerPixel] : 0;
      const above = row > 0 ? pixels[targetOffset + column - rowBytes] : 0;
      const upperLeft = row > 0 && column >= bytesPerPixel
        ? pixels[targetOffset + column - rowBytes - bytesPerPixel]
        : 0;
      pixels[targetOffset + column] = unfilter(filter, source[column], left, above, upperLeft);
    }
  }

  const maxAlpha = header.bitDepth === 8 ? 255 : 65535;
  const alphaValues = [];
  for (let pixel = 0; pixel < header.width * header.height; pixel += 1) {
    const alphaOffset = pixel * bytesPerPixel + 3 * sampleBytes;
    const alpha = sampleBytes === 1 ? pixels[alphaOffset] : pixels.readUInt16BE(alphaOffset);
    alphaValues.push(alpha / maxAlpha);
  }
  return { width: header.width, height: header.height, alphaValues };
}

function unfilter(filter, value, left, above, upperLeft) {
  if (filter === 0) return value;
  if (filter === 1) return (value + left) & 0xff;
  if (filter === 2) return (value + above) & 0xff;
  if (filter === 3) return (value + Math.floor((left + above) / 2)) & 0xff;
  if (filter === 4) return (value + paeth(left, above, upperLeft)) & 0xff;
  throw new Error(`Extension icon PNG uses unsupported filter ${filter}`);
}

function paeth(left, above, upperLeft) {
  const prediction = left + above - upperLeft;
  const leftDistance = Math.abs(prediction - left);
  const aboveDistance = Math.abs(prediction - above);
  const upperLeftDistance = Math.abs(prediction - upperLeft);
  if (leftDistance <= aboveDistance && leftDistance <= upperLeftDistance) return left;
  if (aboveDistance <= upperLeftDistance) return above;
  return upperLeft;
}
