package com.example.demo.Controller;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.example.demo.dto.CompanyVO;
import com.example.demo.dto.FileVO;
import com.example.demo.dto.InventoryVO;
import com.example.demo.dto.MaterialVO;
import com.example.demo.dto.MemberVO;
import com.example.demo.dto.OrderformVO;
import com.example.demo.dto.ProductVO;
import com.example.demo.dto.ProductionDetailVO;
import com.example.demo.dto.ProductionVO;
import com.example.demo.dto.QcDetailVO;
import com.example.demo.dto.QcVO;
import com.example.demo.dto.QuotationDetailVO;
import com.example.demo.dto.QuotationVO;
import com.example.demo.dto.RecipeDetailVO;
import com.example.demo.service.ProjectService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class ProjectController {

	private ModelAndView mv;
	
	@Autowired
	private ProjectService projectService;

	// 홈 화면 (index.html) 이동
	@GetMapping("index")
	public String index(){
	    log.info("index");
	    return "index";
	}
	
	// 로그인화면 이동
	@GetMapping("login")
	public ModelAndView getLogin() {
		mv = projectService.getLogin();
		return mv;
	}
   
	// 로그인
	@PostMapping("login")
	public ModelAndView postLogin(@RequestParam Map<String,Object> map,HttpSession session) {
		mv = projectService.postLogin(map, session);
		return mv;
	}
   
	// 로그아웃
	@GetMapping("logout")
	public String getLogout(HttpSession session) {
		mv = projectService.getLogout(session);
		return "login";
	}
	
	// 회원가입 화면 이동
	@GetMapping("register")
	public String getRegister(){
		log.info("register");
		return "register";
	}
   
	// 회원가입
	@PostMapping("register")
	public ModelAndView postRegister(@RequestParam Map<String,Object> map) {
		mv = projectService.postRegister(map);
		System.out.println(map);
		return mv;
	}
   
	
	// 아이디 중복검사
	@GetMapping("/check-member_id")
	public ResponseEntity<String> checkMember_id(@RequestParam("member_id") String member_id) {
		boolean isTaken = projectService.isMember_idTaken(member_id);
   	
		if (isTaken) {
			return ResponseEntity.ok("이미 사용중인 아이디입니다.");
		} else {
			return ResponseEntity.ok("사용 가능한 아이디입니다.");
		}
	}
   
   
   
	
	

	

	
	
    // 비어있는 페이지
	@GetMapping("notepad")
	public String notepad(){
	    log.info("notepad");
	    return "notepad";
	}	
	
	// 구매계약서 상세 화면 이동
	@GetMapping("quotationDetail")
	public String quotationDetail(){
	    log.info("quotationDetail");
	    
	    return "quotationDetail";
	}	
	
	// company(협력사) 등록 화면 이동
	@GetMapping("companyRegister")
	public String companyRegister(){
	    log.info("companyRegister");
	    
	    return "companyRegister";
	}
	
	// company 등록
	@PostMapping("addCompany")
	public String addCompany(@RequestBody CompanyVO companyVO){
	    String name = companyVO.getCompany_name();
	    int r = projectService.addCompany(companyVO);
	    return "index";
	}
	
	// 제품 등록 화면 이동
	@GetMapping("productRegister")
	public String productRegister(){
	    return "productRegister";
	}
	
	// 제품 등록
    @PostMapping("/addProduct")
    public ResponseEntity<?> addProduct(@RequestBody ProductVO productVO) {
        try {
            // 제품 정보 저장 로직 (예: DB 저장)
            // productService.save(request);
            int r = projectService.addProduct(productVO);
            // inventory에 재고0으로 열만 추가하는 코드
            InventoryVO inventoryVO = new InventoryVO();
            inventoryVO.setInven_item_num(projectService.findMaxProductNum());
            inventoryVO.setInven_name(productVO.getProduct_name());
            int r2 = projectService.addProductInventory(inventoryVO);
            
            // 성공 메시지 반환
            return ResponseEntity.ok(Map.of("message", "Product added successfully"));
        } catch (Exception e) {
            // 오류 발생 시 JSON 에러 메시지 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Failed to add product"));
        }
    }

	

	// 파일(이미지) 업로드를 위한 경로지정
	@Value("${org.zerock.upload.path}")
	private String uploadPath;
	
	// 파일 업로드
    @PostMapping(value = "imageUpload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> imageUpload(@RequestParam(value="fileVO") MultipartFile[] files) {
    	
        try {
        	// 제품의 pk값이 auto-increment로 되어있기 때문에 여기에 1를 더해서 지금 등록한 제품의 pk값을 찾아오려는 과정
        	String maxnum = ""+projectService.findMaxProductNum();
        	
        	// 멀티업로드된 파일 리스트에서 각각의 파일에 대해 데이터베이스에 등록하는 과정
        	for (MultipartFile file : files) {
        		
        		String originalName = file.getOriginalFilename();
        		String uuid = UUID.randomUUID().toString();
        		String uploadName = uuid + "_" + originalName;
        		
        		Path savePath = Paths.get(uploadPath, uploadName);
        		file.transferTo(savePath);
  			
        		FileVO VO = new FileVO();
  			
        		VO.setFile_name(uploadName);
        		VO.setFile_path("images/" + uploadName);

        		VO.setFile_subject("product");
        		VO.setFile_pk(maxnum);
  			
        		int r = projectService.fileUpload(VO);
        	}
            // 성공 메시지 반환
            return ResponseEntity.ok(Map.of("message", "File added successfully"));
            
        } catch (Exception e) {
            // 오류 발생 시 JSON 에러 메시지 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Failed to add file"));
        }
        
    }
    
    // 제품 등록 시 제품코드 중복확인
    @ResponseBody
	@PostMapping("productCodeCheck")
	public int productCodeCheck(@RequestParam("product_code") String product_code) {
	    int cnt = projectService.productCodeCheck(product_code);
	    return cnt;
	}
    
    // 제품 등록 시 입력한 것과 가장 일치하는 협력사 이름을 찾는 과정
    @ResponseBody
	@PostMapping("companyNameCheck")
	public String companyNameCheck(@RequestParam("company_name") String company_name) {
    	company_name = company_name + "%";
	    String company = projectService.companyNameCheck(company_name);
	    return company;
	}
	
    // 구매계약서 등록 화면 이동
    @GetMapping("getOrderformRegister")
    public ModelAndView getOrderformRegister() throws Exception {
    	mv = projectService.getOrderformRegister();
        return mv;
    }
    
    // 구매계약서 등록
    @PostMapping("postOrderformRegister")
    public ModelAndView postOrderformRegister(@RequestParam Map<String,Object> map) throws Exception {
    	
    	
    	mv = projectService.postOrderformRegister(map);
    	List<OrderformVO> list = projectService.orderList();
    	mv.addObject("orderList", list);
        return mv;
    }
    
    // 판매계약서 등록 화면 이동
    @GetMapping("getQuotationRegister")
    public ModelAndView getQuotationRegister() throws Exception {
    	mv = projectService.getQuotationRegister();
        return mv;
    }
    
//    // 판매계약서 등록
//    @PostMapping("postQuotationRegister")
//    public ModelAndView postQuotationRegister(
// 		   @RequestParam Map<String,Object> map
// 		   ) throws Exception {
// 	   mv = projectService.postQuotationRegister(map);
// 	   return mv;
//    }
    
    
    
    
    // 판매계약서 등록
    @PostMapping("postQuotationRegister")
    public String postQuotationRegister(@RequestParam Map<String,Object> map, @RequestParam("count") int count) throws Exception {
    	
    	CompanyVO company1 = projectService.getCompanyByCompanyName((String)map.get("company1"));
    	CompanyVO company2 = projectService.getCompanyByCompanyName((String)map.get("company2"));
    	
    	QuotationVO quotationVO = new QuotationVO();
    	quotationVO.setQuot_name((String)map.get("quot_name"));
    	quotationVO.setQuot_stat((String)map.get("quot_stat"));
    	quotationVO.setCompany_num(company1.getCompany_num());
    	quotationVO.setCompany_num2(company2.getCompany_num());
    	quotationVO.setQuot_content((String)map.get("content"));
    	quotationVO.setQuot_startdate((String)map.get("start_date"));
    	quotationVO.setQuot_enddate((String)map.get("end_date"));
    	
    	int r = projectService.insertQuotation(quotationVO);
    	
    	int quot_num = projectService.getLastQuotationNum();
    	QuotationVO updatedQuotation = projectService.getQuotationByQuotnum(quot_num);
    	
    	String code1 = updatedQuotation.getQuot_regdate().substring(0,10).replaceAll("-", "");
    	String code2 = String.format("%04d", quot_num % 1000);
    	String code3 = String.format("%04d", updatedQuotation.getCompany_num2() % 1000);
    	String code = code1 + code2 + code3;
    	
    	int s = projectService.insertQuotationCode(quot_num, code);
    	
    	for (int i = 1; i <= count; i++) {
    		String a = "item" + i;
    		String b = "unit_price" + i;
    		String c = "quantity" + i;
    		String d = "total_price" + i;
    		
    		QuotationDetailVO quotationDetailVO = new QuotationDetailVO();
    		
    		
			String product_name = (String) (map.get(a));
			String product_price = (String) (map.get(b));
			
			log.info(product_name);
			if (product_name == "") {
				continue;
			}
			if (product_price == "") {
				continue;
			}
			
			int product_num = projectService.getProductByProductName(product_name).getProduct_num();
			int quotdetail_amount = Integer.parseInt((String)map.get(c));
			int quotdetail_price = Integer.parseInt((String)map.get(d));
			
			quotationDetailVO.setQuot_num(quot_num);
    		quotationDetailVO.setProduct_num(product_num);
    		quotationDetailVO.setQuotdetail_amount(quotdetail_amount);
    		quotationDetailVO.setQuotdetail_price(quotdetail_price);
    		
    		int result = projectService.insertQuotationDetail(quotationDetailVO);
    		
    	}
    	
    	return "redirect:salesContract";
    }
    
    
    //판매계약서 상세보기로 이동
    @GetMapping("getQuotationDetail")
    public String getQuotationDetail(
    		@RequestParam("quot_num") int quot_num, Model model
    		) throws Exception {
    	
    	QuotationVO quotationVO = projectService.getQuotationByQuotnum(quot_num);
    	List<QuotationDetailVO> quotationDetailListVO = projectService.getQuotationDetailListByQuotnum(quot_num);
    	
    	int payment = 0;
    	for (QuotationDetailVO quotationDetail : quotationDetailListVO) {
    		payment += quotationDetail.getQuotdetail_price();
    	}
    	model.addAttribute("payment",payment);
    	
		CompanyVO company1VO = projectService.getCompanyByCompanynum(quotationVO.getCompany_num());
		CompanyVO company2VO = projectService.getCompanyByCompanynum(quotationVO.getCompany_num2());
    	
    	model.addAttribute("quotationDetailListVO",quotationDetailListVO);
		model.addAttribute("company1VO",company1VO);
		model.addAttribute("company2VO",company2VO);
		model.addAttribute("quotationVO",quotationVO);
		
        return "quotationDetail";
    }
    
    
    
    
    
    // 입력한 회사명으로 해당 회사의 정보들을 불러오는 과정
    @GetMapping("/getCompanyByCompanyName")
    public ResponseEntity<CompanyVO> getCompanyByCompanyName(
          @RequestParam("company_name") String company_name
          ) {
       CompanyVO company = projectService.getCompanyByCompanyName(company_name);
       
       return ResponseEntity.ok(company);
    }
    
    // 입력한 제품명으로 해당 제품의 정보들을 불러오는 과정
    @GetMapping("/getProductByProductName")
    public ResponseEntity<ProductVO> getProductByProductName(
          @RequestParam("product_name") String product_name
          ) {
       ProductVO product = projectService.getProductByProductName(product_name);
       
       return ResponseEntity.ok(product);
    }
    
    // ----------------------------------------------------------------------------------------
    
    // 협력사 정보 등록 시 협력사 이름 중복 확인
    @ResponseBody
	@PostMapping("companyNameValidation")
	public int companyNameValidation(@RequestParam("company_name") String company_name) {
	    int cnt = projectService.companyNameValidation(company_name);
	    return cnt;
	}
    
	// 협력사 정보 등록 시 협력사 등록번호 중복 확인
    @ResponseBody
	@PostMapping("companyCodeValidation")
	public int companyCodeValidation(@RequestParam("company_code") String company_code) {
	    int cnt = projectService.companyCodeValidation(company_code);
	    return cnt;
	}
    
    // 제품 리스트 화면 이동
    @GetMapping("product")
    public String product(Model model) {
  	   log.info("product");
  	   
  	   List<ProductVO> productList = projectService.productList();
  	   
  	   for (ProductVO product : productList) {
  		   int product_num = product.getProduct_num();
  		   
  		   int amount = projectService.fileAmount(product_num);
  		   
  		   if (amount == 0) {
  			   product.setFile_name("No Image");
  			   product.setFile_path("vendors/images/no-image-alert.jpg");
  		   } else {
  			   FileVO file = projectService.findFirstImage(product_num);
  			   product.setFile_amount(amount);
  			   product.setFile_name(file.getFile_name());
  			   product.setFile_path(file.getFile_path());
  		   }
  		   
  	   }
  	   
  	   
  	   model.addAttribute("productList",productList);
  	   
  	   return "product";
	}
    
    @GetMapping("/getProductDetail")
    public ResponseEntity<?> getProductDetail(@RequestParam("product_num") int product_num, Model model) {
        ProductVO product = projectService.getProductDetail(product_num);
        List<FileVO> files = projectService.getProductImages(product_num);
        int amount = projectService.fileAmount(product_num);
        
        model.addAttribute("product",product);
        
        String[] imagePathArr = new String[amount];
        int cnt = 0;
        
        for (FileVO file : files) {
        	imagePathArr[cnt] = file.getFile_path();
        	cnt++;
        }
        model.addAttribute("imagePathArr",imagePathArr);
        
        if (product != null) {
            return ResponseEntity.ok(model);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
    
    @ResponseBody
    @GetMapping("/getCompanyNameList")
    public String[] getCompanyNameList(Model model) {
    	List<CompanyVO> companyList = projectService.getCompanyList();
    	
    	String[] companyNameList = new String[companyList.size()];
    	int cnt = 0;
    	for (CompanyVO company : companyList) {
    		companyNameList[cnt] = company.getCompany_name();
    		cnt++;
    	}
    	model.addAttribute("companyNameList",companyNameList);
    	
        return companyNameList;
    }
	
    
    @ResponseBody
    @GetMapping("/getProductNameList")
    public String[] getProductNameList(Model model) {
    	log.info("controller access");
    	List<ProductVO> productList = projectService.getProductList();
    	
    	String[] productNameList = new String[productList.size()];
    	int cnt = 0;
    	for (ProductVO product : productList) {
    		productNameList[cnt] = product.getProduct_name();
    		cnt++;
    	}
    	model.addAttribute("productNameList",productNameList);
    	
        return productNameList;
    }
 
    
	// 재고관리 화면 이동
	@GetMapping("inventory")
	public String inventory(Model model){
		
		List<InventoryVO> inventoryList = projectService.getInventoryList();
		model.addAttribute("inventoryList",inventoryList);
		
		List<InventoryVO> inventoryMaterialList = projectService.getInventoryMaterialList();
		model.addAttribute("inventoryMaterialList",inventoryMaterialList);
		
		List<InventoryVO> inventoryProductList = projectService.getInventoryProductList();
		model.addAttribute("inventoryProductList",inventoryProductList);
		
		return "inventory";
	}
    
    
 // 윤호 자리@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@       
    
    
    
    
    // 협력사 목록 화면 이동
    @GetMapping("company")
    public String company(Model model) {
 	   
 	   List<CompanyVO> list = projectService.getCompanyList();
 	   
// 	   for (CompanyVO company : list) {
// 		   company.setCompany_address(simpleAddress(company.getCompany_address()));
// 	   }
 	   
        model.addAttribute("companyList", list);
 	   return "company";
    }
    
    public static String simpleAddress(String address) {
    	if (address.length() < 8) {
    		return "-";
    	}
        String trimmedAddress = address.substring(8);
        String[] words = trimmedAddress.split(" ");

        if (words.length >= 2) {
            return words[0] + " " + words[1];
        }
        return "-";
    }
    
    
	@PostMapping("updateCompany")
	public String updateCompany(@RequestBody CompanyVO companyVO){
	    String name = companyVO.getCompany_name();
	    log.info("Company's name is : "+ name);
	    int r = projectService.updateCompany(companyVO);
	    return "company";
	}
    
	@ResponseBody
	@PostMapping("quotationProceed")
	public int quotationProceed(@RequestParam("quot_num") int quot_num){
	    log.info("Quotation number is : "+ quot_num);
	    List<QuotationDetailVO> quotdetailList = projectService.getQuotationDetailList(quot_num);
	    
	    for (QuotationDetailVO quotdetail : quotdetailList) {
	    	
	    	int product_num = quotdetail.getProduct_num();
	    	int amount = quotdetail.getQuotdetail_amount();
	    	
	    	InventoryVO inventory = projectService.getInventory(product_num);
	    	if (inventory == null) {
	    		return -1;
	    	}
	    	if (amount > inventory.getInven_amount()) {
	    		return -2;
	    	}
	    }
	    
	    for (QuotationDetailVO quotdetail : quotdetailList) {
	    	
	    	int product_num = quotdetail.getProduct_num();
	    	int amount = quotdetail.getQuotdetail_amount();
	    	
	    	int r = projectService.updateInventoryAmount(product_num, amount);
	    }
	    
	    int s = projectService.updateQuotationStat(quot_num);
	    int t = projectService.uploadReleaseDate(quot_num);
	    
	    return 1;
	}
	
	

	
	
	
	
    
    
// 윤호 자리@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    
    
    
    

// -------------------------new 작업공간(이의재) -------------------------
    
//    //판매계약서 상세보기로 이동
//    @GetMapping("getQuotationDetail")
//    public ModelAndView getQuotationDetail(
//    		@RequestParam("quot_num") int quot_num
//    		) throws Exception {
//    	mv = projectService.getQuotationDetail(quot_num);
//        return mv;
//    }
    
    //구매계약서 상세보기로 이동
    @GetMapping("getOrderformDetail")
    public ModelAndView getOrderformDetail(
    		@RequestParam("orderform_num") int orderform_num
    		) {
    	mv = projectService.getOrderformDetail(orderform_num);
    	return mv;
    }
    
    
//    //계약서 List에서 구매,판매 상세보기로 이동
//    @GetMapping("getAllFormDetail")
//    public ModelAndView getAllFormDetail(
//          @RequestParam("this_num") String this_num
//          ) {
//       if (this_num.contains("quot")) {
//          int quot_num = Integer.parseInt(this_num.replaceAll("quot", ""));
//          mv = projectService.getQuotationDetail(quot_num);
//          return mv;
//       } else {
//          int orderform_num = Integer.parseInt(this_num.replaceAll("order", ""));
//          mv = projectService.getOrderformDetail(orderform_num);
//          return mv;
//       }
//    }
    
    
    
    
    //계약서 List에서 구매,판매 상세보기로 이동
    @GetMapping("getAllFormDetail")
    public String getAllFormDetail(
    		@RequestParam("this_num") String this_num
    		) {
    	if (this_num.contains("quot")) {
    		int quot_num = Integer.parseInt(this_num.replaceAll("quot", ""));
    		return "redirect:getQuotationDetail?quot_num="+quot_num;
    	} else {
    		int orderform_num = Integer.parseInt(this_num.replaceAll("order", ""));
    		return "redirect:getOrderformDetail?orderform_num="+orderform_num;
    	}
    }
    
	// 파일 업로드
    @PostMapping(value = "materialImageUpload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> materialImageUpload(@RequestParam(value="fileVO") MultipartFile[] files) {
    	
        try {
        	// 제품의 pk값이 auto-increment로 되어있기 때문에 여기에 1를 더해서 지금 등록한 제품의 pk값을 찾아오려는 과정
        	String maxnum = ""+projectService.findMaxMaterialNum();
        	
        	// 멀티업로드된 파일 리스트에서 각각의 파일에 대해 데이터베이스에 등록하는 과정
        	for (MultipartFile file : files) {
        		
        		String originalName = file.getOriginalFilename();
        		String uuid = UUID.randomUUID().toString();
        		String uploadName = uuid + "_" + originalName;
        		
        		Path savePath = Paths.get(uploadPath, uploadName);
        		file.transferTo(savePath);
  			
        		FileVO VO = new FileVO();
  			
        		VO.setFile_name(uploadName);
        		VO.setFile_path("images/" + uploadName);

        		VO.setFile_subject("material");
        		VO.setFile_pk(maxnum);
  			
        		int r = projectService.fileUpload(VO);
        	}
            // 성공 메시지 반환
            return ResponseEntity.ok(Map.of("message", "File added successfully"));
            
        } catch (Exception e) {
            // 오류 발생 시 JSON 에러 메시지 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Failed to add file"));
        }
        
    }
    
	// 원자재 등록
    @PostMapping("/addMaterial")
    public ResponseEntity<?> addMaterial(@RequestBody MaterialVO materialVO) {
        try {
            // 제품 정보 저장 로직 (예: DB 저장)
            // productService.save(request);
            int r = projectService.addMaterial(materialVO);
            // inventory에 재고0으로 열만 추가하는 코드
            InventoryVO inventoryVO = new InventoryVO();
            inventoryVO.setInven_item_num(projectService.findMaxMaterialNum());
            inventoryVO.setInven_name(materialVO.getMaterial_name());
            int r2 = projectService.addMaterialInventory(inventoryVO);
            // 성공 메시지 반환
            return ResponseEntity.ok(Map.of("message", "Product added successfully"));
        } catch (Exception e) {
            // 오류 발생 시 JSON 에러 메시지 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Failed to add product"));
        }
    }

    // 제품 리스트 화면 이동
    @GetMapping("Material")
    public String Material(Model model) {
  	   
  	   List<MaterialVO> MaterialList = projectService.getMaterialList();
  	   
  	   for (MaterialVO material : MaterialList) {
  		   int material_num = material.getMaterial_num();
  		   
  		   int amount = projectService.materialFileAmount(material_num);
  		   
  		   if (amount == 0) {
  			   material.setFile_name("No Image");
  			   material.setFile_path("vendors/images/product-img1.jpg");
  		   } else {
  			   FileVO file = projectService.materialFindFirstImage(material_num);
  			   material.setFile_amount(amount);
  			   material.setFile_name(file.getFile_name());
  			   material.setFile_path(file.getFile_path());
  		   }
  		   
  	   }
  	   
  	   
  	   model.addAttribute("MaterialList",MaterialList);
  	   
  	   return "material";
	}
    
    @GetMapping("/getMaterialDetail")
    public ResponseEntity<?> getMaterialDetail(@RequestParam("material_num") int material_num, Model model) {
        MaterialVO material = projectService.getMaterialDetail(material_num);
        List<FileVO> files = projectService.getMaterialImages(material_num);
        int amount = projectService.materialFileAmount(material_num);
        
        model.addAttribute("material",material);
        
        String[] imagePathArr = new String[amount];
        int cnt = 0;
        
        for (FileVO file : files) {
        	imagePathArr[cnt] = file.getFile_path();
        	cnt++;
        }
        model.addAttribute("imagePathArr",imagePathArr);
        
        if (material != null) {
            return ResponseEntity.ok(model);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
    
    @ResponseBody
    @GetMapping("/getMaterialNameList")
    public String[] getMaterialNameList(Model model) {
    	log.info("controller access");
    	List<MaterialVO> materialList = projectService.getMaterialList();
    	
    	String[] productNameList = new String[materialList.size()];
    	int cnt = 0;
    	for (MaterialVO product : materialList) {
    		productNameList[cnt] = product.getMaterial_name();
    		cnt++;
    	}
    	model.addAttribute("productNameList",productNameList);
    	
        return productNameList;
    }
    
    // 입력한 원자재명으로 해당 원자재의 정보들을 불러오는 과정
    @GetMapping("/getMaterialByMaterialName")
    public ResponseEntity<MaterialVO> getMaterialByMaterialName(
          @RequestParam("product_name") String product_name
          ) {
       MaterialVO product = projectService.getMaterialByMaterialName(product_name);
       
       return ResponseEntity.ok(product);
    }
    
    
    //원자재 등록 페이지로 이동
    @GetMapping("getMaterialRegister")
    public ModelAndView getMaterialRegister(
    		
    		) {
    	mv = projectService.getMaterialRegister();
    	return mv;
    }
    
    // 원자재 등록 시 원자재 코드 중복확인
    @ResponseBody
	@PostMapping("materialCodeCheck")
	public int materialCodeCheck(@RequestParam("material_code") String material_code) {
	    int cnt = projectService.materialCodeCheck(material_code);
	    return cnt;
	}
    
    //멤버 리스트로 이동
    
    @GetMapping("member")
    public ModelAndView member(
    		
    		) {
    	mv = projectService.member();
    	return mv;
    }
    
    @GetMapping("/getMemberByMemberId")
    public ResponseEntity<MemberVO> getMemberByMemberID (
    		@RequestParam("member_id") String member_id
    		) {
    	MemberVO member = projectService.getMemberByMemberId(member_id);
    	
    	return ResponseEntity.ok(member);
    }
    
    // 멤버 수정시 아이디 중복 확인
    @ResponseBody
	@PostMapping("memberIdValidation")
	public int memberIdValidation(
			@RequestParam Map<String,Object> map
			) {
	    int cnt = projectService.memberIdValidation(map);
	    return cnt;
	}
    
    @PostMapping("/updateMember")
    public String updateMember(@RequestBody MemberVO memberVO) {
    	int result = projectService.updateMember(memberVO);
    	return "index";
    }
    
    @GetMapping("getRecipeRegister")
    public String getRecipeRegister() {
    	return "recipeRegister";
    }
    
 // -------------------------new 작업공간(이의재) -------------------------    
    
    
 // 박나현 시작. ------------------------------
    
    // QC 리스트 페이지로 이동
    @GetMapping("qc")
    public String qc(Model model) {
    	List<QcVO> QcList = projectService.getQcList();
    	model.addAttribute("QcList", QcList);
    	log.info("qc 이동");
        return "qc";
    }

    // QC 상세 페이지로 이동
    @GetMapping("qcDetail")
    public String qcDetail(@RequestParam("qc_num") int qc_num, Model model) {
  
    	log.info("qcDetail 이동");
    	log.info("qc_num = "+qc_num);
    	
    	QcVO qc = projectService.getOneQc(qc_num);
  
    	// 타입 저장, 이름 담아줄 변수 생성
    	String qctype = qc.getQc_type(); // 타입을 게또    	
    	String item_name = null; // name은 qc에 들어가지 않고 직접 뿌려줍니다
    	
    	// 타입 조건문 ==> 이름 찾아옴
    	if ("order".equals(qctype)) {
    		item_name = projectService.getQcMName(qc_num);	
    		log.info("order로 넘어왔음, item_name == " + item_name);
    	} else if ("plan".equals(qctype)) {
    		item_name = projectService.getQcPName(qc_num);
    		log.info("plan로 넘어왔음, item_name == " + item_name);
    	} else {
    		log.info("통과했슈");
    	}
    	
    	// 상세 내역 불러오기
    	List<QcVO> QcDetailList = projectService.getOneQcDetail(qc_num);
    	int totalQC = qc.getQc_quan();
    	int totalFail = projectService.getTotalFail(qc_num);
    	int totalPass = totalQC - totalFail;
    	double failRate = (double)totalFail/totalQC*100;
    	
    	model.addAttribute("qc", qc);
    	model.addAttribute("item_name", item_name);
    	model.addAttribute("qc_num", qc_num);
    	model.addAttribute("QcDetailList", QcDetailList);
    	model.addAttribute("totalQC", totalQC);
    	model.addAttribute("totalFail", totalFail);
    	model.addAttribute("totalPass", totalPass);
    	model.addAttribute("failRate", failRate);
    	
        return "qcDetail";
    }


    // QC 수정 페이지 이동
    @GetMapping("qcTest")
    public String qcTest(@RequestParam("qc_num") int qc_num, Model model) {
    	
    	log.info("qcTest 이동");
    	log.info("qc_num = "+qc_num);

    	QcVO qc = projectService.getOneQc(qc_num);
    	  
    	// 타입 저장, 이름 담아줄 변수 생성
    	String qctype = qc.getQc_type(); // 타입을 게또    	
    	String item_name = null; // name은 qc에 들어가지 않고 직접 뿌려줍니다
    	
    	// 타입 조건문 ==> 이름 찾아옴
    	if ("order".equals(qctype)) {
    		item_name = projectService.getQcMName(qc_num);	
    		log.info("order로 넘어왔음, item_name == " + item_name);
    	} else if ("plan".equals(qctype)) {
    		item_name = projectService.getQcPName(qc_num);
    		log.info("plan로 넘어왔음, item_name == " + item_name);
    	} else {
    		log.info("통과했슈");
    	}
    	
    	// 상세 내역 불러오기
    	List<QcVO> QcDetailList = projectService.getOneQcDetail(qc_num);
    	int totalQC = qc.getQc_quan();
    	int totalFail = projectService.getTotalFail(qc_num);
    	int totalPass = totalQC - totalFail;
    	double failRate = (double)totalFail/totalQC*100;
    	
    	model.addAttribute("qc", qc);
    	model.addAttribute("item_name", item_name);
    	model.addAttribute("qc_num", qc_num);
    	model.addAttribute("QcDetailList", QcDetailList);
    	model.addAttribute("totalQC", totalQC);
    	model.addAttribute("totalFail", totalFail);
    	model.addAttribute("totalPass", totalPass);
    	model.addAttribute("failRate", failRate);
    	
        return "qcTest";
    }    
    
    // QC Test 부적격 수량 저장
    @ResponseBody
    @PostMapping("updateQcDetail")    
    public ResponseEntity<String> updateQcDetail(@RequestBody List<QcDetailVO> qcDetails) {
        try {
            for (QcDetailVO detail : qcDetails) {
                log.info("QC 문항 번호: " + detail.getQcq_num());
                log.info("부적격 수량: " + detail.getQc_fail_quan());
                int isQcDetail = projectService.isQcDetail(detail); // 값 존재하는지 확인
                
                if (isQcDetail == 0) {
                	projectService.insertQcDetail(detail);
                } else if (isQcDetail == 1) {
                    projectService.updateQcDetail(detail);
                }
                projectService.updateQcStat1(detail.getQc_num()); // 상태를 '작성중'으로 변경
            }
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
        	log.error("Error 발생 : ", e); // 에러 로그
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }
    
    // QC Test POST
    @PostMapping("qcTest")
    public String qcTest(@RequestParam Map<String, String> map,
						Model model) { 	
    	System.out.println(map);    	
    	String inven_name = map.get("item_name");
        String qc_type = map.get("qc_type");
        
        int inven_item_num = Integer.parseInt(map.get("qc_item_num"));
        int inven_amount = Integer.parseInt(map.get("totalPass"));
        int qc_num = Integer.parseInt(map.get("qc_num"));
        
//        System.out.println("########################## inven_name: " + invenName);
        System.out.println("########################## qc_type: " + qc_type);
        System.out.println("########################## inven_item_num: " + inven_item_num);
        System.out.println("########################## inven_amount: " + inven_amount);
        System.out.println("########################## qc_num: " + qc_num);
    	
    	InventoryVO inven = new InventoryVO();
    	
//    	inven.setInven_name(invenName);
    	
    	// 타입
    	int inven_type = 0;
	    	if (qc_type.equals("order")) {
	    		inven_type = 0;
	    	} else if (qc_type.equals("plan")) {
	    		inven_type = 1;
	    	}
	    	
	    inven.setInven_name(inven_name);
	    inven.setInven_item_num(inven_item_num);
	    inven.setInven_amount(inven_amount);
	    inven.setInven_type(inven_type);
	    
	    System.out.println(inven.getInven_item_num());
	    
    	int updateinven = projectService.updateInven(inven);
    	int updateQcStat2 = projectService.updateQcStat2(qc_num);
    	
    	List<QcVO> QcList = projectService.getQcList();
    	model.addAttribute("QcList", QcList);
    	log.info("qc 이동");
    	
    	return "redirect:/qc";
    }
    
    
    
	// company 등록
	@PostMapping("addCompanytest")
	public String addCompanytest(@RequestBody CompanyVO companyVO){
	    String name = companyVO.getCompany_name();
	    log.info("Company's name is : "+ name);
	    int r = projectService.addCompany(companyVO);
	    return "index";
	}
    
    
    // QC 유형 등록 페이지 이동
    @GetMapping("qcTypeReg")
    public String qcTypeReg() {
    	log.info("qcTypeReg 이동");
        return "qcTypeReg";
    }
    


// 박나현. 끝. ------------------------------


    //================ 김민성 ============================================================================
    
    // 생산계획서 폼 upload
    @PostMapping("postProductionForm")
    public String postProductionForm(@RequestParam Map<String,Object> formData) {
          log.info("formData",formData);
       ProductionVO productionVO = new ProductionVO();
       productionVO.setPd_writer((String)formData.get("pd_writer"));
       productionVO.setPd_dept((String)formData.get("pd_dept"));
       productionVO.setPd_startdate((String)formData.get("pd_startdate"));
       productionVO.setPd_enddate((String)formData.get("pd_enddate"));
       productionVO.setPd_name((String)formData.get("pd_name"));
       productionVO.setPd_content((String)formData.get("pd_content"));
       
       projectService.insertProduction(productionVO);
       
       int num = (int) projectService.getfindLastProductionNumber();
       //num 은 pd_num 생성되자마자 넣음
       //============================================================= 여기까지는 바로 진행해야함.
       //maxCount 추출
       int maxCount = Integer.parseInt((String) formData.get("maxCount"));
       // num = pd_num 
       System.out.println(maxCount);
       
       List<ProductionDetailVO> list = new ArrayList<>();
       
       for(int i =1; i<=maxCount;i++) {
          String itemNameKey = "item_name" + i ; 
          String quantityKey = "quantity" + i ;
          log.info(itemNameKey);
          log.info(quantityKey);
          
          if(formData.containsKey(itemNameKey) && formData.containsKey(quantityKey)) {
             String itemName = (String) formData.get(itemNameKey);
             int quantity = Integer.parseInt((String) formData.get(quantityKey));
             
             ProductionDetailVO planVO = new ProductionDetailVO();
             planVO.setPd_num(num);
             planVO.setProduct_name(itemName);
             planVO.setProductiondetail_amount(quantity);
             
             list.add(planVO);
             
             QcVO qcVO = new QcVO();
              
               qcVO.setQc_type("plan");
               qcVO.setPaper_num(num);
               qcVO.setQc_writer("test");
               qcVO.setQc_item_num(planVO.getPd_num());
               qcVO.setQc_quan(quantity);
               
               int result2 = projectService.insertqc(qcVO);
             
          }
       }
    
       list.forEach(planVO ->
          log.info("list -> Pd num: {}, Item Name: {},Quantity: {}",planVO.getPd_num(), planVO.getProduct_name(), planVO.getProductiondetail_amount())
             );
       
       int r = projectService.setproductionForm(list);
       
       
       
       return "redirect:productionPlan";
    }
    // 구매계약서 목록 화면 이동
    @GetMapping("purchaseContract")
    public String purchaseContract(Model model) {
 	   
    	List<OrderformVO> list = projectService.orderList();
        model.addAttribute("orderList", list);
        log.info("list",list);
        return "purchaseContract";
        
    }
    
    // 판매계약서 목록 화면 이동
    @GetMapping("salesContract")
    public String salesContractList(Model model) {
 	   
 	   List<QuotationVO> list = projectService.quotationList();
 	   
 	   for (QuotationVO quotationVO : list) {
 		   String company_name1 = projectService.getCompanyByCompanynum(quotationVO.getCompany_num()).getCompany_name();
 		   String company_name2 = projectService.getCompanyByCompanynum(quotationVO.getCompany_num2()).getCompany_name();
 		   quotationVO.setCompany_name1(company_name1);
 		   quotationVO.setCompany_name2(company_name2);
 		   
 	   }
 	   
 	   
        model.addAttribute("quotationList", list);
        log.info("list",list);
 	   return "salesContract";
    }
    
    // 모든 계약서들의 목록을 볼 수 있는 화면 이동
    @GetMapping("allForm")
    public String allFormList(Model model) {
    	List<QuotationVO> list = projectService.allFormList();
    	model.addAttribute("AllFormList", list);
    	log.info("allFormList",list);
        return "allForm";
    }
	// 생산계획서 목록 화면 이동
    @GetMapping("productionPlan")
    public String getproductionPlanList(Model model) {
 	   
 	   List<ProductionVO> list = projectService.getProductionList();
 	   model.addAttribute("getProductionPlanList", list);
 	  log.info("getProductionPlanList",list);
 	   return "productionPlan";
    }
	// 생산계획서 폼 이동
    @GetMapping("productionForm")
    public String productionForm() {
 	   
 	  log.info("productionForm()");
 	   return "productionForm";
    }
   
    //facotry.html
    @GetMapping("factoryPlan")
    public String factoryPlan(Model model) {
    	List<ProductionVO> list = projectService.getProductionList();
    	model.addAttribute("getProductionPlanList", list);
    	  log.info("factoryPlan",list);
    	return "factoryPlan";
    }
    @GetMapping("getFactoryDetail")
    public ModelAndView getFactoryDetail(@RequestParam("pd_num") int pd_num) {
    	
    	ProductionVO productionVO = new ProductionVO();
    	productionVO = projectService.getFactoryDetail(pd_num);
    	List<ProductionVO> productionListVO = projectService.getFactoryDetailList(pd_num);
    	
    	mv = new ModelAndView();
    	mv.addObject("productionVO",productionVO);
    	mv.addObject("productionList",productionListVO);
    	mv.setViewName("factoryDetail");
    	
    	
    
    	return mv;
    }
    
    @PostMapping("postFactoryDetail")
    public String postFactoryDetail(@RequestParam Map<String,Object> formData) {
    	
        Map<String,Object> itemData = formData.entrySet()
                .stream()
                .filter(entry -> entry.getKey().contains("item_name"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
          
          Optional<Integer> itemMaxNumber = itemData.keySet()
                .stream()
                .filter(key -> key.startsWith("item_name"))
                .map(key -> Integer.parseInt(key.replace("item_name", "")))
                .max(Integer::compareTo);
         
          
        int maxCount = itemMaxNumber.orElse(0);
        
    	int num = (int) projectService.getfindLastProductionNumber();
    	
    	for(int i =1; i<=maxCount;i++) {
    		String itemNameKey = "item_name" + i ; 
    		String quantityKey = "quantity" + i ;
    		
    		String itemName = (String) formData.get(itemNameKey);
    		int quantity = Integer.parseInt((String) formData.get(quantityKey));
    		
		
			 log.info(itemNameKey);
			 log.info(quantityKey);
			 
    		
    		int recipe_num = projectService.getRecipeNumByProductName(itemName);
    		List<RecipeDetailVO> list = projectService.getRecipeDetailListByRecipeNum(recipe_num);
    		int listSize = list.size();
    		
    		for(int k =1; k<=listSize;k++) {
    			 int Mamount =list.get(k-1).getMaterial_amount();
    			String Mname = list.get(k-1).getMaterial_name();
    			InventoryVO inventoryVO = new InventoryVO();
    			
    			int totalMamount = Mamount * quantity ; 
    		
    			inventoryVO.setInven_amount(totalMamount);
    			inventoryVO.setInven_name(Mname);
    			
    			int r = projectService.reduceInventoryAmount(inventoryVO);
    		}
    		
    	}
    	
    	
    	return "redirect:factoryPlan";
    }
    
    
    
    
    
    
}
