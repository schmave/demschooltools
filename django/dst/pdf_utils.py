import shutil
import tempfile
from pathlib import Path

from django.conf import settings
from django.http import HttpResponse
from playwright.sync_api import sync_playwright


def copy_print_assets_to_temp_dir(temp_dir: Path) -> None:
    shutil.copytree(
        # In production, we want to copy the files from STATIC_ROOT because
        # they have the hash included in them. In dev, STATIC_ROOT is None
        Path(settings.STATIC_ROOT or settings.STATICFILES_DIRS[0]) / "css",
        temp_dir / "css",
    )


def prepare_html_for_pdf(html_content: str, temp_dir: Path) -> str:
    return html_content.replace(f"{settings.STATIC_URL}css/", "css/")


def render_html_to_pdf(html_content: str) -> bytes:
    DEBUG = False
    with tempfile.TemporaryDirectory(delete=not DEBUG) as temp_dir_str:
        temp_dir = Path(temp_dir_str)
        copy_print_assets_to_temp_dir(temp_dir)

        prepared_html = prepare_html_for_pdf(html_content, temp_dir)

        html_file = temp_dir / "document.html"
        html_file.write_text(prepared_html, encoding="utf-8")

        if DEBUG:
            import os

            os.system(f"open '{temp_dir}'")

        with sync_playwright() as p:
            browser = p.chromium.launch()
            page = browser.new_page()

            page.goto(f"file://{html_file}")

            pdf_bytes = page.pdf(
                format="Letter",
                print_background=True,
                margin={"top": "1in", "right": "1in", "bottom": "1in", "left": "1in"},
            )

            browser.close()
            return pdf_bytes


def create_pdf_response(pdf_bytes: bytes, filename: str) -> HttpResponse:
    response = HttpResponse(pdf_bytes, content_type="application/pdf")
    response["Content-Disposition"] = f'inline; filename="{filename}"'
    return response
