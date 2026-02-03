#!/usr/bin/env python3
"""
脚本：重新对齐native库以支持16KB页面大小
"""

import sys
import os
import shutil
import struct
import zipfile

PAGE_SIZE_16KB = 16384

def align_file(input_file, output_file, alignment):
    """对齐文件到指定的边界"""
    file_size = os.path.getsize(input_file)
    padding = (alignment - (file_size % alignment)) % alignment

    with open(input_file, 'rb') as f:
        data = f.read()

    # 添加填充
    if padding > 0:
        data += bytes(padding)

    with open(output_file, 'wb') as f:
        f.write(data)

    return True

def align_apk(apk_path, output_path):
    """对齐APK中的native库"""
    temp_dir = "temp_align"

    # 清理临时目录
    if os.path.exists(temp_dir):
        shutil.rmtree(temp_dir)

    os.makedirs(temp_dir)

    print("解压APK...")
    with zipfile.ZipFile(apk_path, 'r') as zip_ref:
        zip_ref.extractall(temp_dir)

    # 对齐so文件
    print("重新对齐native库...")
    for root, dirs, files in os.walk(os.path.join(temp_dir, "lib")):
        for file in files:
            if file.endswith(".so"):
                so_path = os.path.join(root, file)
                print(f"对齐: {file}")

                # 计算对齐
                file_size = os.path.getsize(so_path)
                padding = (PAGE_SIZE_16KB - (file_size % PAGE_SIZE_16KB)) % PAGE_SIZE_16KB

                if padding > 0:
                    # 添加填充
                    with open(so_path, 'rb') as f:
                        data = f.read()
                    data += bytes(padding)

                    with open(so_path, 'wb') as f:
                        f.write(data)

                    print(f"  添加了 {padding} 字节填充")
                else:
                    print(f"  文件已经对齐")

    # 重新打包APK
    print("重新打包APK...")
    with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zip_out:
        for root, dirs, files in os.walk(temp_dir):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, temp_dir)
                zip_out.write(file_path, arcname)

    # 清理临时目录
    shutil.rmtree(temp_dir)

    print(f"完成！对齐后的APK: {output_path}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("用法: python align_native_libs.py <apk_path> [output_path]")
        sys.exit(1)

    apk_path = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 else apk_path.replace(".apk", "_aligned.apk")

    if not os.path.exists(apk_path):
        print(f"错误: 文件不存在: {apk_path}")
        sys.exit(1)

    align_apk(apk_path, output_path)

