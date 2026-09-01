from PIL import Image
import glob

def remove_borders(path):
    img = Image.open(path).convert("RGBA")
    pixels = img.load()
    width, height = img.size
    
    changed = False
    for x in range(width):
        for y in range(height):
            r, g, b, a = pixels[x, y]
            # Check for dark border pixels (often dark blue/black in this mod's art style)
            if r < 30 and g < 30 and b < 50 and a > 200:
                nx, ny = x, y
                if x == 0: nx = 1
                elif x == width - 1: nx = width - 2
                if y == 0: ny = 1
                elif y == height - 1: ny = height - 2
                
                nr, ng, nb, na = pixels[nx, ny]
                if nr < 30 and ng < 30 and nb < 50:
                    if x == 0: nx = 2
                    elif x == width - 1: nx = width - 3
                    if y == 0: ny = 2
                    elif y == height - 1: ny = height - 3
                
                pixels[x, y] = pixels[nx, ny]
                changed = True
    
    if changed:
        img.save(path)
        print(f"Fixed borders in: {path}")

for file in glob.glob("src/main/resources/assets/planeshift/textures/block/course_*_block.png"):
    remove_borders(file)
remove_borders("src/main/resources/assets/planeshift/textures/block/brick_block.png")
