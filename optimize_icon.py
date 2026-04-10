import os
from PIL import Image

source_icon = "e:\\Code\\Script\\Fuel_consumption_record\\icon.png"

targets = [
    ("e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\mipmap-mdpi", 48),
    ("e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\mipmap-hdpi", 72),
    ("e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\mipmap-xhdpi", 96),
    ("e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\mipmap-xxhdpi", 144),
    ("e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\mipmap-xxxhdpi", 192),
]

def center_crop_to_square(img):
    """将图像中心裁剪为正方形"""
    width, height = img.size
    if width == height:
        return img  # 已经是正方形
    
    # 计算裁剪区域
    new_size = min(width, height)
    left = (width - new_size) // 2
    top = (height - new_size) // 2
    right = left + new_size
    bottom = top + new_size
    
    return img.crop((left, top, right, bottom))

try:
    img = Image.open(source_icon)

    # 转换为 RGB（处理透明通道）
    if img.mode in ('RGBA', 'LA', 'P'):
        background = Image.new('RGB', img.size, (255, 255, 255))
        if img.mode == 'P':
            img = img.convert('RGBA')
        background.paste(img, mask=img.split()[-1] if img.mode == 'RGBA' else None)
        img = background

    # 先中心裁剪为正方形
    img = center_crop_to_square(img)

    for target_dir, size in targets:
        # 现在可以安全地 resize（因为已经是正方形）
        resized_img = img.resize((size, size), Image.LANCZOS)
        
        ic_launcher_path = os.path.join(target_dir, "ic_launcher.png")
        ic_launcher_round_path = os.path.join(target_dir, "ic_launcher_round.png")

        # 确保目录存在
        os.makedirs(target_dir, exist_ok=True)

        resized_img.save(ic_launcher_path, "PNG", optimize=True)
        resized_img.save(ic_launcher_round_path, "PNG", optimize=True)

        print(f"Created {size}x{size} icons in {target_dir}")

    print("Icon optimization completed successfully!")

except Exception as e:
    print(f"Error: {e}")