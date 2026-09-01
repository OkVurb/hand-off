from PIL import Image, ImageEnhance
import os

source = 'src/main/resources/assets/planeshift/textures/environment/course_skybox.png'
if not os.path.exists(source):
    print("Source not found")
    exit(1)

img = Image.open(source).convert('RGBA')

themes = {
    'grass': (255, 255, 255),
    'desert': (255, 230, 180),
    'snow': (200, 220, 255),
    'lava': (255, 150, 100),
    'underground': (50, 50, 50),
    'ghost_house': (80, 50, 100)
}

for name, color in themes.items():
    tinted = Image.new('RGBA', img.size)
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = img.getpixel((x, y))
            r = int(r * color[0] / 255.0)
            g = int(g * color[1] / 255.0)
            b = int(b * color[2] / 255.0)
            tinted.putpixel((x, y), (r, g, b, a))
    dest = f'src/main/resources/assets/planeshift/textures/environment/course_skybox_{name}.png'
    tinted.save(dest)
    print(f"Saved {dest}")
