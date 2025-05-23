from datetime import datetime, timedelta

from dotenv import load_dotenv
from langchain_core.tools import StructuredTool
from langchain_openai import ChatOpenAI
from langchain.tools import tool
import dateparser
import re
import json

from openai import BaseModel

load_dotenv()
llm = ChatOpenAI(temperature=0, model="gpt-4o")

def parse_natural_time(expression: str):
  now = datetime.now()
  start = None
  end = None
  expr = expression.lower().strip()

  # 显式规则优先
  if expr in ["last week", "past week"]:
    start = now - timedelta(days=now.weekday() + 7)
    end = start + timedelta(days=6)
  elif expr in ["this week", "current week"]:
    start = now - timedelta(days=now.weekday())
    end = start + timedelta(days=6)
  elif expr in ["last month", "previous month"]:
    first_day_this_month = datetime(now.year, now.month, 1)
    last_month_end = first_day_this_month - timedelta(days=1)
    start = datetime(last_month_end.year, last_month_end.month, 1)
    end = last_month_end
  else:
    # 尝试用 dateparser 解析
    parsed = dateparser.parse(expression)
    if parsed:
      start = parsed
      end = parsed
    else:
      # fallback：使用 LLM 推理时间范围
      prompt = f"""
You are a strict JSON formatter.

Convert the user's natural language time expression — "{expression}" — into a JSON object with exactly two keys: "start_date" and "end_date".

Today's date is {now.strftime('%Y-%m-%d')}.  
If the start time is not mentioned, default to 3 months before today.  
If the end time is not mentioned, default to today's date.

Important formatting rules:
- The output MUST be a plain JSON object.
- DO NOT use markdown formatting (no ```json or ```).
- DO NOT include any explanatory text, headers, or surrounding characters.

The output format must be:

{{
  "start_date": "YYYY/MM/DD",
  "end_date": "YYYY/MM/DD"
}}

Return only the raw JSON. Nothing else.
"""

      response = llm.predict(prompt)
      try:
        json_result = json.loads(response)
        return json_result["start_date"], json_result["end_date"]
      except Exception as e:
        raise ValueError(f"LLM failed to parse date expression '{expression}'. Raw output: {response}")

  return start.strftime("%Y-%m-%d"), end.strftime("%Y-%m-%d")

class TimeRangeInput(BaseModel):
  prompt: str

def resolve_time_range(prompt: str) -> dict:
  """Convert natural language time expressions (e.g., 'last week', 'next month') into start and end date strings."""
  start, end = parse_natural_time(prompt)
  return {"start_time": start, "end_time": end}

resolve_time_tool = StructuredTool.from_function(
    name="resolve_time_range",
    description=(
      "Parse a natural language expression (e.g., 'past three months', 'last week') "
      "into a time range. The `prompt` parameter should contain the user's input. "
      "Returns a JSON object with 'start_date' and 'end_date' in 'YYYY-MM-DD' format."
    ),
    func=resolve_time_range,
    args_schema=TimeRangeInput
)
