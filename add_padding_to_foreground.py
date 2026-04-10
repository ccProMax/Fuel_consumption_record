import os
from PIL import Image

# 前景图标路径
foreground_icons = [
    "e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\drawable\\ic_launcher_foreground.png",
    "e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\drawable\\ic_launcher_foreground1.png",
]

# 内边距（像素）
padding = 20  # 108的1/9，这是Android推荐的内边距

try:
    for icon_path in foreground_icons:
        # 打开图标
        img = Image.open(icon_path)

        # 确保图像有透明通道
        if img.mode != 'RGBA':
            img = img.convert('RGBA')

        # 创建新的图像，添加内边距
        new_size = 108+padding  # 标准尺寸
        new_img = Image.new('RGBA', (new_size, new_size), (0, 0, 0, 0))

        # 计算粘贴位置（居中）
        paste_x = (new_size - img.size[0]) // 2
        paste_y = (new_size - img.size[1]) // 2

        # 将原始图像粘贴到新图像上
        new_img.paste(img, (paste_x, paste_y), img)

        # 保存图像
        new_img.save(icon_path, "PNG", optimize=True)

        print(f"Added padding to {icon_path}")

    print("Padding added successfully!")

except Exception as e:
    print(f"Error: {e}")
