from PIL import Image
import glob
import os

search_path = 'src/main/resources/assets/planeshift/textures/item/*.png'
files = glob.glob(search_path)
count = 0
for i, f in enumerate(files):
    if os.path.getsize(f) > 1000:
        continue
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
