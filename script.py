from PIL import Image
import os
import glob

source_img = 'C:/Users/cr0od/.gemini/antigravity/brain/20b09b30-6630-44b4-97d3-83af12129290/nano_banana_1788289934130.jpg'
try:
    img = Image.open(source_img).resize((16, 16), Image.Resampling.NEAREST).convert('RGBA')
except Exception as e:
    print(e)
    exit(1)

# Find all small PNGs in textures/item
search_path = 'C:/Dev/PlaneShift/src/main/resources/assets/planeshift/textures/item/*.png'
count = 0
for f in glob.glob(search_path):
    if os.path.getsize(f) < 400:
        img.save(f)
        count += 1
print(f'Replaced {count} placeholder item textures with nano banana.')
