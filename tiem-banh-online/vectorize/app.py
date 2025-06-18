import gradio as gr
import subprocess
from transformers import pipeline

# Khởi tạo text generator (chỉ cần chạy một lần)
gen = pipeline("text2text-generation", model="google/flan-t5-base")

def rag_system(question):
    # Gọi script search.py để lấy context từ db
    result = subprocess.run(
        ["python", "search.py", question],
        cwd=".",
        capture_output=True, text=True
    )
    context = result.stdout.strip()
    if not context:
        return "Không tìm thấy thông tin liên quan."
    prompt = f"Dựa trên thông tin sau:\n{context}\nTrả lờdi: {question}"
    out = gen(prompt, max_length=200)[0]['generated_text']
    return out

# Xây dựng giao diện Gradio
iface = gr.Interface(
    fn=rag_system,
    inputs="text",
    outputs="text",
    title="RAG Chatbot",
    description="Dựa trên dữ liệu đã build, hỏi và trả lời nhanh!"
)
iface.launch()
