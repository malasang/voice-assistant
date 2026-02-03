#!/usr/bin/env python3
"""
验证APK中的native库是否对齐到16KB页面大小
"""

import zipfile
import sys

def verify_alignment(apk_path):
    """验证APK中的native库对齐情况"""
    print(f"验证APK: {apk_path}")
    print("=" * 60)

    try:
        with zipfile.ZipFile(apk_path, 'r') as zip_ref:
            aligned_count = 0
            total_count = 0

            for file_info in zip_ref.filelist:
                if file_info.filename.endswith('.so'):
                    total_count += 1
                    file_size = file_info.file_size
                    remainder = file_size % 16384
                    is_aligned = remainder == 0

                    if is_aligned:
                        aligned_count += 1
                        status = "✅ 对齐"
                    else:
                        status = f"❌ 未对齐 (余数: {remainder})"

                    print(f"{file_info.filename}")
                    print(f"  大小: {file_size:,} 字节")
                    print(f"  状态: {status}")
                    print()

            print("=" * 60)
            print(f"总计: {total_count} 个native库")
            print(f"对齐: {aligned_count} 个")
            print(f"未对齐: {total_count - aligned_count} 个")

            if aligned_count == total_count:
                print("\n✅ 所有native库都已对齐到16KB页面大小！")
                return True
            else:
                print(f"\n❌ 还有 {total_count - aligned_count} 个库未对齐")
                return False

    except Exception as e:
        print(f"❌ 错误: {e}")
        return False

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("用法: python verify_alignment.py <apk_path>")
        sys.exit(1)

    apk_path = sys.argv[1]
    verify_alignment(apk_path)

