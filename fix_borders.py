from PIL import Image
import sys

def remove_borders(path):
    img = Image.open(path).convert("RGBA")
    pixels = img.load()
    width, height = img.size
    print(f"Loaded {path} ({width}x{height})")
    
    # Get interior colors to replace borders
    for x in range(width):
        for y in range(height):
            r, g, b, a = pixels[x, y]
            # Check if it's very dark (like a black border)
            if r < 30 and g < 30 and b < 50 and a > 200:
                # Replace with a nearby pixel
                nx, ny = x, y
                if x == 0: nx = 1
                elif x == width - 1: nx = width - 2
                if y == 0: ny = 1
                elif y == height - 1: ny = height - 2
                
                # if still black, move further
                nr, ng, nb, na = pixels[nx, ny]
                if nr < 30 and ng < 30 and nb < 50:
                    if x == 0: nx = 2
                    elif x == width - 1: nx = width - 3
                    if y == 0: ny = 2
                    elif y == height - 1: ny = height - 3
                
                pixels[x, y] = pixels[nx, ny]
    
    img.save(path)
    print(f"Saved {path}")

remove_borders("src/main/resources/assets/planeshift/textures/block/course_grass_block.png")
remove_borders("src/main/resources/assets/planeshift/textures/block/course_cloud_block.png")
