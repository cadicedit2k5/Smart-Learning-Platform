# Sử dụng langchain docling nên không cần split thủ công nữa, sau này có nhu cầu mở lại sau

# from langchain_core.documents import Document
# from langchain_text_splitters import RecursiveCharacterTextSplitter
#
#
# class DocumentsSplitter:
#     def __init__(self, chunk_size: int, chunk_overlap: int):
#         self.splitter = RecursiveCharacterTextSplitter(chunk_size=chunk_size, chunk_overlap=chunk_overlap,
#                                                        add_start_index=True)
#
#
#     def split(self, documents: list[Document]):
#         return self.splitter.split_documents(documents)