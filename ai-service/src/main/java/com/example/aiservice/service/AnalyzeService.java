package com.example.aiservice.service;

import com.example.aiservice.feign.ProductClient;
import com.example.aiservice.response.InteriorDesignResponse;
import com.example.aiservice.response.ProductResponse;
import com.example.aiservice.response.ApiResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyzeService {

    private final ChatClient chatClient;
    private final ProductClient productClient; // Đã thêm Feign Client

    // 1. System Prompt: Định hình chuyên gia FurniMart
    private static final String SYSTEM_PROMPT = """
            Bạn là kiến trúc sư nội thất cao cấp của hệ thống FurniMart.
            Nhiệm vụ: Phân tích không gian và BÁN HÀNG.
            Hãy chọn các sản phẩm phù hợp nhất TỪ KHO HÀNG CỦA FURNIMART để tư vấn cho khách.
            Lời khuyên phải thực tế, thẩm mỹ và tôn trọng ngân sách.
            Trả lời bằng Tiếng Việt chuyên nghiệp.
            """;

    // Constructor Injection: Phải đưa cả ProductClient vào đây
    public AnalyzeService(ChatClient.Builder chatClientBuilder, ProductClient productClient) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
        this.productClient = productClient;
    }

    public InteriorDesignResponse analyzeRoom(MultipartFile file, String userNote) {
        // 1. Validate File
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Vui lòng upload file ảnh hợp lệ.");
        }
        Resource resource = file.getResource();
        var mimeType = MimeTypeUtils.parseMimeType(contentType);

        // 2. Lấy danh sách sản phẩm từ Product Service (Microservice khác)
        // Lưu ý: Trong thực tế nên dùng RAG hoặc filter bớt nếu danh sách quá dài (>100 món)
        ApiResponse<List<ProductResponse>> response = productClient.getProducts();
        List<ProductResponse> products = response.getData(); // Giả sử ApiResponse có field 'result' hoặc 'data'

        if (products == null || products.isEmpty()) {
            throw new RuntimeException("Kho hàng đang trống, không thể tư vấn sản phẩm!");
        }

        // 3. Biến đổi danh sách Product thành String gọn nhẹ để AI đọc
        // Chỉ lấy các trường cần thiết để tiết kiệm Token
        String productContext = formatProductsForAI(products);

        // 4. User Prompt: Yêu cầu chọn hàng từ danh sách trên
        String userPromptText = """
                Hãy phân tích hình ảnh căn phòng này và thực hiện các bước:
                
                1. [Phân tích]: Xác định phong cách thiết kế và bảng màu hiện tại.
                
                2. [Tư vấn sản phẩm]: Dựa vào danh sách sản phẩm trong kho dưới đây, hãy CHỌN RA 3 SẢN PHẨM phù hợp nhất để đặt vào phòng này giúp đẹp hơn.
                
                --- DANH SÁCH SẢN PHẨM TRONG KHO ---
                %s
                ------------------------------------
                
                3. [Vị trí]: Với mỗi sản phẩm đã chọn, hãy chỉ rõ nên đặt nó ở đâu trong phòng (góc trái, cạnh cửa sổ, giữa phòng...).
                
                Yêu cầu khách hàng: "%s"
                
                LƯU Ý QUAN TRỌNG VỀ OUTPUT:
                - Trả về JSON thuần túy (không Markdown).
                - Key giữ nguyên tiếng Anh: style, analysis, colorPalette, suggestions (itemName, reason, placementAdvice).
                - Trong 'itemName': Phải ghi rõ Tên sản phẩm kèm Mã ID (VD: Sofa Da Bò - ID: P0123).
                """.formatted(productContext, userNote != null ? userNote : "Tối ưu thẩm mỹ");

        // 5. Gọi AI
        return chatClient.prompt()
                .user(u -> u.text(userPromptText)
                        .media(mimeType, resource))
                .call()
                .entity(InteriorDesignResponse.class);
    }

    // Hàm phụ trợ: Biến List Object thành String
    private String formatProductsForAI(List<ProductResponse> products) {
        // Giới hạn 50 sản phẩm đầu tiên để tránh lỗi quá tải Token (Context Window Limit)
        // Nếu muốn xịn hơn thì phải dùng Vector Database để tìm kiếm trước.
        return products.stream()
                .limit(50)
                .map(p -> {
                    // Lấy thông tin màu sắc nếu có
                    String colors = (p.getProductColors() != null) ?
                            p.getProductColors().stream()
                                    .map(c -> c.getColor().getColorName()) // Giả sử ColorResponse có getName()
                                    .collect(Collectors.joining(", ")) : "N/A";

                    return String.format("- ID: %s | Tên: %s | Giá: %.0f | Màu: %s | Loại: %s",
                            p.getId(),
                            p.getName(),
                            p.getPrice(),
                            colors,
                            p.getCategoryName());
                })
                .collect(Collectors.joining("\n"));
    }


    private static final String SYSTEM_PROMPT_OPTIMIZATION = """
            Bạn là chuyên gia điều phối và tối ưu hóa chuỗi cung ứng FurniMart. 
            Nhiệm vụ của bạn là phân tích dữ liệu nhu cầu (Demand) và tồn kho (Supply) và 
            đề xuất hành động chuyển kho tối ưu nhất để cân bằng stock, giảm thiểu hết hàng.
            LUÔN LUÔN trả về kết quả dưới dạng JSON hợp lệ theo schema yêu cầu.
            """;



//    public String generateTransferSuggestion(String targetProductColorId) {
//        String salesHistory = dataClient.getSalesHistoryForForecasting(targetProductColorId);
//        String currentInventory = dataClient.getCurrentStockOverview(targetProductColorId);
//
//        // 2. Gom dữ liệu vào 1 prompt duy nhất
//        String optimizationPromptText = String.format("""
//            Phân tích và đưa ra đề xuất chuyển kho cho sản phẩm ID: %s
//
//            --- DỮ LIỆU CẦN PHÂN TÍCH ---
//            Lịch sử bán hàng gần đây (7 ngày): %s
//            Tồn kho hiện tại: %s
//            ---
//
//            QUY TẮC:
//            1. Ưu tiên cân bằng tồn kho. Kho nào có "Forecasted Demand" cao hơn "Current Stock" (dựa trên lịch sử) cần được bổ sung.
//            2. Kho nguồn nên là kho có "Current Stock" cao nhất và "Sales History" thấp nhất.
//
//            ĐẦU RA BẮT BUỘC phải là JSON theo schema sau (không markdown, không giải thích):
//            {
//              "transferRecommended": boolean,
//              "transferDetails": {
//                "fromWarehouseId": "string",
//                "toWarehouseId": "string",
//                "quantityToTransfer": number,
//                "reason": "string"
//              }
//            }
//            """, targetProductColorId, salesHistory, currentInventory);
//
//        try {
//            return chatClient.prompt()
//                    .user(optimizationPromptText)
//                    .call()
//                    .content();
//        } catch (Exception e) {
//            return "{\"transferRecommended\": false, \"reason\": \"Lỗi kết nối AI Service\"}";
//        }
//    }
}