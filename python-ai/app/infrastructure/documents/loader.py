import asyncio
from pathlib import Path

from docling.datamodel.base_models import InputFormat
from docling.datamodel.object_detection_engine_options import TransformersObjectDetectionEngineOptions
from docling.datamodel.pipeline_options import PdfPipelineOptions, LayoutObjectDetectionOptions
from docling.document_converter import DocumentConverter, PdfFormatOption
from langchain_core.documents import Document
from langchain_docling import DoclingLoader
from langchain_docling.loader import ExportType

class DocumentLoader:

    def __init__(self):
        # Disable compile model to compatible with the env.
        layout_options = LayoutObjectDetectionOptions(
            engine_options=TransformersObjectDetectionEngineOptions(
                compile_model=False,
            ),
        )

        pdf_options = PdfPipelineOptions(
            layout_options=layout_options,
        )

        self.SUPPORTED_EXTENSIONS = {
            ".pdf",
            ".docx",
            ".txt",
        }

        self.converter = DocumentConverter(
            format_options={
                InputFormat.PDF: PdfFormatOption(
                    pipeline_options=pdf_options,
                ),
            },
        )
        # Không cho quá nhiều PDF chạy đồng thời trong cùng process.
        self._docling_slots = asyncio.Semaphore(1)

    async def load(self, file_path: Path) -> list[Document]:
        if not file_path.is_file():
            raise FileNotFoundError(f"Document not found: {file_path}")
        
        suffix = file_path.suffix.lower()

        if suffix not in self.SUPPORTED_EXTENSIONS:
            raise ValueError(f"Unsupported document type: {suffix}")

        return await asyncio.to_thread(
            self._load_with_docling,
            file_path,
        )

    def _load_with_docling(
        self,
        file_path: Path,
    ) -> list[Document]:
        loader = DoclingLoader(
            file_path=str(file_path),
            converter=self.converter,
            export_type=ExportType.DOC_CHUNKS,
        )

        return loader.load()