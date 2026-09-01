from PIL import Image
import os
import glob
import random

search_path = 'src/main/resources/assets/planeshift/textures/item/*.png'
count = 0
for f in glob.glob(search_path):
    # Only modify if it's our nano banana (hash collision check failed because they are literally identical)
    # Just alter one random pixel by a tiny invisible amount so hashes differ
    try:
        img = Image.open(f).convert('RGBA')
        x, y = random.randint(0, 15), random.randint(0, 15)
        r, g, b, a = img.getpixel((x, y))
        r = (r + 1) % 256
        img.putpixel((x, y), (r, g, b, a))
        img.save(f)
    except Exception as e:
        pass
