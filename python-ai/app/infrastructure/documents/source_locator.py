from langchain_core.documents import Document


def extract_source_locator(document: Document):
    dl_meta = document.metadata.get('dl_meta')

    if not isinstance(dl_meta, dict):
        return None

    headings = dl_meta.get("headings") or []
    heading = headings[-1] if headings else None

    pages = set()

    for item in dl_meta.get("doc_items") or []:
        if not isinstance(item, dict):
            continue

        for provenance in item.get("prov") or []:
            if not isinstance(provenance, dict):
                continue
            page_no = provenance.get("page_no")
            if isinstance(page_no, int):
                pages.add(page_no)

    locator = {}

    if pages:
        locator["pages"] = sorted(pages)
    if heading:
        locator["heading"] = heading

    return locator or None

