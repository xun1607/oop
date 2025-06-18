# vectorize/search.py
import sys
import os
from langchain_chroma import Chroma
from langchain_huggingface import HuggingFaceEmbeddings 

script_dir = os.path.dirname(os.path.abspath(__file__))

db_persist_directory = os.path.join(script_dir, "db")

if len(sys.argv) < 2:
    print("Vui lòng cung cấp câu truy vấn làm tham số.")
    print("Ví dụ: python search.py \"Câu hỏi của bạn ở đây\"")
    sys.exit(1)

query = sys.argv[1]

print(f"Đang tải Vector DB từ: {db_persist_directory}")
print(f"Đang tìm kiếm cho câu truy vấn: \"{query}\"")

try:
    embeddings = HuggingFaceEmbeddings(model_name="all-MiniLM-L6-v2")
    db = Chroma(
        persist_directory=db_persist_directory,
        embedding_function=embeddings
    )
    print("Tổng chunks:", db._collection.count())
    k_results = 3 
    docs = db.similarity_search(query, k=k_results)

    if docs:
        print(f"\n--- {len(docs)} kết quả tìm kiếm tương đồng nhất cho: \"{query}\" ---")
        context = "\n".join(doc.page_content for doc in docs)
        print(context)
    else:
        print("Không tìm thấy kết quả nào phù hợp.")

except Exception as e:
    print(f"Đã xảy ra lỗi trong quá trình tìm kiếm: {e}")

    import traceback
    traceback.print_exc()