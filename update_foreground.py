import os
from PIL import Image

# 源图标路径
source_icon = "e:\\Code\\Script\\Fuel_consumption_record\\icon.png"

# 目标目录和尺寸
target_dir = "e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\drawable"
size = 108  # 自适应图标前景的标准尺寸

try:
    # 打开源图标
    img = Image.open(source_icon)

    # 转换为RGB模式（如果是RGBA）
    if img.mode in ('RGBA', 'LA', 'P'):
        # 创建透明背景
        background = Image.new('RGBA', img.size, (255, 255, 255, 0))
        if img.mode == 'P':
            img = img.convert('RGBA')
        background.paste(img, mask=img.split()[-1] if img.mode == 'RGBA' else None)
        img = background

    # 调整大小
    resized_img = img.resize((size, size), Image.LANCZOS)

    # 保存为PNG
    ic_launcher_foreground_path = os.path.join(target_dir, "ic_launcher_foreground.png")
    resized_img.save(ic_launcher_foreground_path, "PNG", optimize=True)

    print(f"Created {size}x{size} foreground icon in {target_dir}")
    print("Foreground icon update completed successfully!")

except Exception as e:
    print(f"Error: {e}")
