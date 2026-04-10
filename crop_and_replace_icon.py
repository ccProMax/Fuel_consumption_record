import os
from PIL import Image, ImageOps

# 源图标路径
source_icon = "e:\\Code\\Script\\Fuel_consumption_record\\icon.png"

# 目标目录和尺寸
targets = [
    ("e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\mipmap-mdpi", 48),
    ("e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\mipmap-hdpi", 72),
    ("e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\mipmap-xhdpi", 96),
    ("e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\mipmap-xxhdpi", 144),
    ("e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\mipmap-xxxhdpi", 192),
]

# 自适应图标前景尺寸
foreground_size = 108
foreground_dir = "e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\drawable"

try:
    # 打开源图标
    img = Image.open(source_icon)

    # 转换为RGB模式（如果是RGBA）
    if img.mode in ('RGBA', 'LA', 'P'):
        # 创建白色背景
        background = Image.new('RGB', img.size, (255, 255, 255))
        if img.mode == 'P':
            img = img.convert('RGBA')
        background.paste(img, mask=img.split()[-1] if img.mode == 'RGBA' else None)
        img = background

    # 裁剪为正方形（中心裁剪）
    width, height = img.size
    min_side = min(width, height)

    # 计算裁剪区域
    left = (width - min_side) // 2
    top = (height - min_side) // 2
    right = left + min_side
    bottom = top + min_side

    # 裁剪图片
    img = img.crop((left, top, right, bottom))

    # 为每个目标尺寸创建图标
    for target_dir, size in targets:
        # 调整大小
        resized_img = img.resize((size, size), Image.LANCZOS)

        # 保存为PNG
        ic_launcher_path = os.path.join(target_dir, "ic_launcher.png")
        ic_launcher_round_path = os.path.join(target_dir, "ic_launcher_round.png")

        resized_img.save(ic_launcher_path, "PNG", optimize=True)
        resized_img.save(ic_launcher_round_path, "PNG", optimize=True)

        print(f"Created {size}x{size} icons in {target_dir}")

    # 创建自适应图标前景
    foreground_img = img.resize((foreground_size, foreground_size), Image.LANCZOS)
    ic_launcher_foreground_path = os.path.join(foreground_dir, "ic_launcher_foreground.png")
    foreground_img.save(ic_launcher_foreground_path, "PNG", optimize=True)
    print(f"Created {foreground_size}x{foreground_size} foreground icon in {foreground_dir}")

    print("Icon replacement completed successfully!")

except Exception as e:
    print(f"Error: {e}")
