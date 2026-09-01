from PIL import Image
import glob
import os

search_path = 'src/main/resources/assets/planeshift/textures/item/*.png'
files = [f for f in glob.glob(search_path) if os.path.getsize(f) < 400]
count = 0
for i, f in enumerate(files):
    try:
        img = Image.open(f).convert('RGBA')
        x, y = i % 16, i // 16
        r, g, b, a = img.getpixel((x, y))
        r = (r + 1) % 256
        img.putpixel((x, y), (r, g, b, a))
        img.save(f)
        count += 1
    except Exception as e:
        pass
print(f'Fixed {count} textures.')
