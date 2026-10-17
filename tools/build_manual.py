# -*- coding: utf-8 -*-
"""Generate the organizer manual .docx."""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from manual_builder import Manual, create_document, save
from manual_content_front import build_front
from manual_content_mid import build_ch4, build_ch5, build_ch6, build_ch7
from manual_content_back import build_ch8, build_ch9, build_ch10, build_appendix


OUTPUT = r"D:\project\beer-competition\docs\05-用户手册\啤酒赛事平台-主办方全流程操作手册-V1.0.docx"


def main():
    doc = create_document()
    m = Manual(doc)
    build_front(m)
    m.page_break()
    build_ch4(m)
    build_ch5(m)
    build_ch6(m)
    build_ch7(m)
    build_ch8(m)
    build_ch9(m)
    build_ch10(m)
    build_appendix(m)
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    save(doc, OUTPUT)
    print(f"Generated: {OUTPUT}")


if __name__ == "__main__":
    main()
