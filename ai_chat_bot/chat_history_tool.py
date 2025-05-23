def format_chat_history_for_llm(chat_history: list[dict]) -> str:
  lines = []
  for turn in chat_history:
    if "user" in turn:
      lines.append(f"User: {turn['user']}")
    if "reply" in turn:
      lines.append(f"Assistant: {turn['reply']}")
  return "\n".join(lines)