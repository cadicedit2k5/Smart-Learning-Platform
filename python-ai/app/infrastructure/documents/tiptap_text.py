BLOCK_TYPES = {
    "doc",
    "paragraph",
    "heading",
    "blockquote",
    "listItem",
    "bulletList",
    "orderedList",
    "codeBlock",
}


def extract_tiptap_text(node: object) -> str:
    if isinstance(node, list):
        return "\n".join(filter(None, (extract_tiptap_text(item) for item in node)))

    if not isinstance(node, dict):
        return ""

    node_type = node.get("type")

    if node_type == "text":
        return str(node.get("text", ""))

    if node_type == "hardBreak":
        return "\n"

    children = node.get("content")

    if not isinstance(children, list):
        return ""

    parts = [extract_tiptap_text(child) for child in children]
    parts = [part for part in parts if part.strip()]

    separator = "\n" if node_type in BLOCK_TYPES else " "

    return separator.join(parts)