from PIL import Image

def process(src, dest):
    img = Image.open(src).convert("RGB")
    
    # The actual block seems to be roughly inside the 100 to 924 range based on the image size
    # I'll just resize the whole thing to 16x16 without crop first, it's easier and usually mostly the block.
    # Actually, crop 128,128 to 896,896 roughly?
    img = img.crop((100, 100, 924, 924))
    
    # Downscale to 16x16 with Nearest Neighbor for pixel art look
    img = img.resize((16, 16), Image.NEAREST)
    img.save(dest)
    print("Saved", dest)

process("C:/Users/cr0od/.gemini/antigravity/brain/20b09b30-6630-44b4-97d3-83af12129290/donut_block_ai_1788220181604.jpg", "src/main/resources/assets/planeshift/textures/block/donut_block.png")
