package com.example.chatproj.chatproj.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.chatproj.chatproj.domain.Chatlog_Table;
import com.example.chatproj.chatproj.domain.Chatroom_Table;
import com.example.chatproj.chatproj.domain.Fileupload_Table;
import com.example.chatproj.chatproj.domain.UC_Table;
import com.example.chatproj.chatproj.domain.User;
import com.example.chatproj.chatproj.domain.User_Profileimg;
import com.example.chatproj.chatproj.service.UserService;

import ch.qos.logback.classic.net.SyslogAppender;

import com.example.chatproj.chatproj.service.ChatService;

@Controller
public class controller {

	private final UserService userService;
	private final ChatService chatService;
	
	@Autowired
	ServletContext application;
	
	@Autowired
	public controller(UserService userService, ChatService chatService) {
		this.userService = userService;
		this.chatService = chatService;
	}
	
	// 메인페이지
	@RequestMapping("/")
	public String main(HttpServletRequest request) {
		// session
		HttpSession session = request.getSession();
		String sessionName = (String)session.getAttribute("sessionId");	
	
		if(sessionName != null) {
			return "redirect:/chatList";
		}else {
			return "redirect:/signin";
		}
	}
	
	// 회원가입
	@RequestMapping("/signup")
	public String signup() {
		return "signup";
	}
	
	@PostMapping("/signup")
	public String create_user(Model model, SignupForm form, userimgForm userimgform, RedirectAttributes redirectAttributes) throws Exception {
		User user = new User();
		user.setUid(form.getUid());
		user.setUpw(form.getUpw());
		user.setUname(form.getUname());
		user.setEmail(form.getEmail());
		user.setPhone_num(form.getPhone_num());
		
		try {
			userService.join(user);
		}catch(IllegalStateException e){		
			if(e.getMessage().equals("이미 존재하는 아이디입니다.")) {
				return "redirect:/signup?message=duplicateId";
			}else if(e.getMessage().equals("이미 존재하는 이메일입니다.")){
				return "redirect:/signup?message=duplicateEmail";
			}
		}
		
		Optional<User> getUidbyUserinform = userService.getSessionbyUid(form.getUid());
		int imageUsernum = getUidbyUserinform.get().getUnum();
		
		// 회원가입 때 이미지 파일 가져오기
		userimgform.getUserimg();
		
		System.out.println("img : " + userimgform.getUserimg().getOriginalFilename());
		
		if(userimgform.getUserimg().getOriginalFilename().equals("")) {
			User_Profileimg userimgfile = (User_Profileimg) nomralinsert(imageUsernum);			
			userService.userimgjoin(userimgfile);	
			
			return "redirect:/signin";	
		}else {
			User_Profileimg userimgfile = (User_Profileimg) fileinsert(userimgform.getUserimg(), imageUsernum);			
			userService.userimgjoin(userimgfile);	
			
			return "redirect:/signin";	
		}
	}
	//기본 이미지 insert
	public User_Profileimg nomralinsert(int imageUsernum){
		User_Profileimg userimg = new User_Profileimg();
		
		String originalfilename = "normal_img.png";
		String fileurl = "/userimg/";
		
		userimg.setFilename(originalfilename);
		userimg.setOriginal_filename(originalfilename);
		userimg.setFile_url(fileurl);
		userimg.setUnum(imageUsernum);		
				
		return userimg;
	}
	
	//이미지 파일 insert
	public User_Profileimg fileinsert(@RequestPart MultipartFile files, int imageUsernum) throws Exception{
		User_Profileimg userimg = new User_Profileimg();
		
		String originalfilename = files.getOriginalFilename();
		String originalfilenameExtension = FilenameUtils.getExtension(originalfilename).toLowerCase();
		File destinationfile;
		String destinationfilename;
		String fileurl = "/userimg/";
		String savePath = application.getRealPath(fileurl);
		
		do {
			destinationfilename = RandomStringUtils.randomAlphanumeric(32) + "." + originalfilenameExtension;
			destinationfile = new File(savePath, destinationfilename);
		}while(destinationfile.exists());
		
		//destinationfile.getParentFile().mkdirs();
		try {
			files.transferTo(destinationfile);
		}catch (IOException e) {
			// TODO: handle exception
		}
		
		userimg.setFilename(destinationfilename);
		userimg.setOriginal_filename(originalfilename);
		userimg.setFile_url(fileurl);
		userimg.setUnum(imageUsernum);
		
		return userimg;
		
	}
	
	// 로그인
	@RequestMapping("/signin")
	public String signin() {
		return "signin";
	}
	
	@PostMapping("/signin")
	public String login_user(SigninForm form, HttpServletRequest request) {
		User user = new User();
		user.setUid(form.getUid());
		user.setUpw(form.getUpw());
		
		String result = userService.login(user);
		
		if(result.equals("matchX")) {
			return "redirect:/signin?message=FAILURE_matchX";
		}else if(result.equals("noid")) {
			return "redirect:/signin?message=FAILURE_noid";
		}else {
			HttpSession session = request.getSession();
			String name = form.getUid();
			session.setAttribute("sessionId", name);
			return "redirect:/chatList";
		}	
	}
	
	// 로그아웃
	@PostMapping("/signout")
	public String signout(HttpServletRequest request){
		HttpSession session = request.getSession();
		session.invalidate();
		return "redirect:/signin";
		
	}
	
	// 유저 처리 페이지
	@RequestMapping("/userprocess")
	public String uesrprocess(@RequestParam("redirectprocess") String redirectprocess) {
		
		return "UserProcess";
	}

	// 아이디 찾기
	@RequestMapping("/findid")
	public String findid() {
		return "find_id";
	}
	
	@PostMapping("/findid")
	public String find_userid(FindIdForm form, RedirectAttributes redirectAttributes) {
		User user = new User();
		user.setUname(form.getUname());
		user.setEmail(form.getEmail());
		
		String redirectprocess = null;
		
		try {
			String result = userService.findUser(user);
			redirectprocess = result;
		
		}catch(NoSuchElementException e) {
			redirectprocess = "존재하지 않는 ID 입니다.";
		}
		redirectAttributes.addAttribute("redirectprocess", redirectprocess);
		return "redirect:/userprocess";	
	}
	
	// 비밀번호 찾기
	@RequestMapping("/findpw")
	public String findpass() {
		return "find_password";
	}
	
	// 채팅방 리스트
	@RequestMapping("/chatList")
	public String chatlist(HttpServletRequest request, Model model) {
		HttpSession session = request.getSession();
		String sessionName = (String)session.getAttribute("sessionId");
	
		// 세션 select
		Optional<User> getSessionName = userService.getSessionbyUid(sessionName);		
		int sessionNum = getSessionName.get().getUnum();
			
			// 세션 기반 채팅방리스트 가져오기
			List<UC_Table> getChatList = chatService.getChatList(sessionNum);
			
			HashMap<Integer, String> map = new HashMap<Integer, String>();
			
			for(int i = 0; i<getChatList.size(); i++) {
				map.put(getChatList.get(i).getCnum(), getChatList.get(i).getCname());
			}
			model.addAttribute("chatlist", map);
			
		return "chatList";
	}
	
	@PostMapping("/chatList")
	public String chatlistadd(HttpServletRequest request, RedirectAttributes redirectAttributes) {
		String submitList = request.getParameter("list");
		String splitsubmitList[] = submitList.split("[.]");
		
		String submitListPKstr = splitsubmitList[0];
		int submitListPK = Integer.parseInt(submitListPKstr);
		
		String submitListName = splitsubmitList[1];
		
		List<UC_Table> stringToinfo = chatService.getstringToinfo(submitListPK, submitListName);
		
		int cnumPK = stringToinfo.get(0).getCnum();
		redirectAttributes.addAttribute("cnumPK", cnumPK);
		
		return "redirect:/chat";
	}
	
	
	
	
	
	// 초대
	@RequestMapping("/inviteuser")
	public String inviteuser(HttpServletRequest request) {
		// session
		HttpSession session = request.getSession();
		String sessionName = (String)session.getAttribute("sessionId");	
		
		if(sessionName != null) {
			return "InviteUser";
		}else {
			return "redirect:signin";
		}
		
	}
	
	@PostMapping("/inviteuser")
	public String invite_user(SigninForm signinform, InviteUserForm inviteuserform, HttpServletRequest request) {
		User user = new User();
		user.setUid(signinform.getUid());
		
		String id_check_result = userService.duplicateMatch(user);
		
//		if(id_check_result.equals("matchX")) {
//			return "redirect:/inviteuser?message=FAILURE_matchX";
//		}else 
		if(id_check_result.equals("noid")) {
			return "redirect:/inviteuser?message=FAILURE_noid";
		}else {
			// session
			HttpSession session = request.getSession();
			String sessionName = (String)session.getAttribute("sessionId");	
			// insert chatroom_table
			if(inviteuserform.getUid() != sessionName && !inviteuserform.getUid().equals(sessionName)) {
				Optional<Chatroom_Table> selectcnum = chatService.join();
				
				Chatroom_Table chatroom_table = new Chatroom_Table();
				
				try {
					chatroom_table.setCnum(selectcnum.get().getCnum() + 1);
					chatroom_table.setCname(inviteuserform.getCname());			
					chatService.insChatTable(chatroom_table);
				}catch(NoSuchElementException e) {
					chatroom_table.setCnum(1);
					chatroom_table.setCname(inviteuserform.getCname());			
					chatService.insChatTable(chatroom_table);		
				}
				
				// select m from user_table m where uid in ( 내 id값, 초대한 사람의 id값);
				String inviteuser = inviteuserform.getUid();
				
				List<User> GetMyIdInviteId = userService.getIdbyUid(sessionName, inviteuser);
				
				// session, invite 기반 insert			
				for(int i = 0; i<GetMyIdInviteId.size(); i++) {
					UC_Table uc_table = new UC_Table();
					uc_table.setUnum(GetMyIdInviteId.get(i).getUnum());
					try {
						uc_table.setCnum(selectcnum.get().getCnum() + 1);
						uc_table.setCname(inviteuserform.getCname());	
					}catch(NoSuchElementException e){
						Optional<Chatroom_Table> null_chatroom_table = chatService.join();		
						uc_table.setCnum(null_chatroom_table.get().getCnum());
						uc_table.setCname(inviteuserform.getCname());
					}
					chatService.insUCTable(uc_table);
				}
			
			}else {
				return "redirect:/inviteuser?message=FAILURE_sameId";				
			}
			
			return "redirect:/chatList";
		}	
	
	}
	
	// 채팅방
	@RequestMapping("/chat")
	public String chatpg(Model model, @RequestParam("cnumPK") int cnumPK, @RequestParam(value="msg", required=false) String msg, @RequestParam(value="nowtime", required=false) String nowtime, HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes) throws IOException {
		// session
		HttpSession session = request.getSession();
		String sessionName = (String)session.getAttribute("sessionId");	
		
		// sessionName to sessionNum
		Optional<User> getSessionName = userService.getSessionbyUid(sessionName);		
		int sessionNum = getSessionName.get().getUnum();
		
		List<UC_Table> getUnum = chatService.getUserInfo(cnumPK);
		
		int unumPk[] = new int[getUnum.size()];
		
		for(int i=0; i<getUnum.size(); i++) {
			unumPk[i] = getUnum.get(i).getUnum();
		}
		
		int match = 0;
		while(true) {
			if(sessionNum != unumPk[match]) {
				match++;
				if(match == getUnum.size()) {
					return "redirect:/signin";
				}
			}else {
				break;
			}
		}

		// ▲ 비정상적인 접근 차단
	
		// log 조회
		List<Chatlog_Table> chatlog = chatService.getChatLog(cnumPK);
		Optional<Chatroom_Table> cname = chatService.getChatName(cnumPK);
		
		model.addAttribute("cname", cname.get().getCname());
		model.addAttribute("sessionName", sessionName);
		model.addAttribute("sessionNum", sessionNum);
		model.addAttribute("chatlog",chatlog);
		model.addAttribute("cnumPK", cnumPK);
		
		// log 조회(img)
		
		
		Optional<User_Profileimg> user_profileimage = userService.getUnumbyFilenum(sessionNum);
		Optional<User_Profileimg> filename = userService.findimage(user_profileimage.get().getFilenum());
		String userimg = filename.get().getFilename();
		
		model.addAttribute("userimg", userimg);
		
		// get ajax data -> insert logtable
		Chatlog_Table chatlog_table = new Chatlog_Table();		
		try {
			if(!msg.equals("") && msg != null) {
				chatlog_table.setUnum(sessionNum);
				chatlog_table.setCnum(cnumPK);
				chatlog_table.setLog(msg);
				chatlog_table.setTime(null);
				chatlog_table.setUname(sessionName);
				chatlog_table.setFilename(userimg);
				chatlog_table.setTime(nowtime);
				
				chatService.logjoin(chatlog_table);
			}
		}catch(NullPointerException e) {
			
		}
		
		//filelist
		List<Fileupload_Table> fileinfo = chatService.getfileinfo(cnumPK);
			
			ArrayList<String> map1 = new ArrayList<>();
			
			for(int i=0; i<fileinfo.size(); i++) {
				map1.add(fileinfo.get(i).getUname() + ","
						+ fileinfo.get(i).getFilename() + ","
						+ fileinfo.get(i).getOriginal_filename() + "," 
						+ fileinfo.get(i).getTime() + "/");
			}
			
			String map = String.join("", map1);
			
			model.addAttribute("filelist", map);
		
		
		return "chat";
	}
	
//	@RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
//	@ResponseBody
//	public ResponseEntity<?> uploadFile(
//	    @RequestParam("uploadfile") MultipartFile uploadfile) {
//	  
//	  try {
//	    // Get the filename and build the local file path (be sure that the 
//	    // application have write permissions on such directory)
//	    String filename = uploadfile.getOriginalFilename();
//	    String directory1 = "/uploadfile/";
//	    String directory = application.getRealPath(directory1);
//	    String filepath = Paths.get(directory, filename).toString();
//	    
//	    System.out.println("ffffffffffffffffff" + filepath);
//	    
//	    // Save the file locally
//	    BufferedOutputStream stream =
//	        new BufferedOutputStream(new FileOutputStream(new File(filepath)));
//	    stream.write(uploadfile.getBytes());
//	    stream.close();
//	  }
//	  catch (Exception e) {
//	    System.out.println(e.getMessage());
//	    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//	  }
//	  
//	  return new ResponseEntity<>(HttpStatus.OK);
//	} // method uploadFile
	
	@PostMapping("/uploadFile")
	public String upload_file( RedirectAttributes redirectAttributes, HttpServletRequest request, FileuploadForm form) throws Exception {
		Fileupload_Table fileupload = new Fileupload_Table();
		fileupload.setCnum(form.getCnum());
		fileupload.setUnum(form.getUnum());
		
		SimpleDateFormat nowTimes = new SimpleDateFormat("yyyy-mm-dd_HH:mm");
		Calendar now = Calendar.getInstance();
		String time = nowTimes.format(now.getTime());	
		fileupload.setTime(time);
		
		Optional<User> unum = userService.findByNum(form.getUnum());
		fileupload.setUname(unum.get().getUname());
	
		MultipartFile files = form.getFileupload();
		String originalfilename = files.getOriginalFilename();
		String originalfilenameExtension = FilenameUtils.getExtension(originalfilename).toLowerCase();
		File destinationfile;
		String destinationfilename;
		String fileurl = "/uploadfile/";
		String savePath = application.getRealPath(fileurl);
		
		do {
			destinationfilename = RandomStringUtils.randomAlphabetic(32) + "." + originalfilenameExtension;
			destinationfile = new File(savePath, destinationfilename);
		}while(destinationfile.exists());
		
		try {
			files.transferTo(destinationfile);
		}catch(IOException e) {
			
		}
		
		fileupload.setFilename(destinationfilename);
		fileupload.setOriginal_filename(originalfilename);
		fileupload.setFile_url(savePath);
		
		chatService.fileuploadjoin(fileupload);
		
		redirectAttributes.addAttribute("cnumPK", form.getCnum());
		return "redirect:chat";
	}
	
	@RequestMapping("/chatexit")
	public String exitbtn(HttpServletRequest request, @RequestParam("cnumPK") int cnumPK) throws IOException {
		// session
		HttpSession session = request.getSession();
		String sessionName = (String)session.getAttribute("sessionId");	
		
		// sessionName to sessionNum
		Optional<User> getSessionName = userService.getSessionbyUid(sessionName);		
		int sessionNum = getSessionName.get().getUnum();
	
		// 채팅방 나가기 로직
		int getCnumPK = cnumPK;
		chatService.exitUser(cnumPK, sessionNum);
		List<UC_Table> getUserInfo = chatService.getUserInfo(getCnumPK);
		
		if(getUserInfo.size() == 0) {
			chatService.deleteChatRoom(getCnumPK);
		}
		
		return "redirect:chatList";
	}
	
	@RequestMapping("/2")
	public String filedowntest(Model model) {
		List<Fileupload_Table> fileinfo = chatService.getfileinfo(2);
		
		ArrayList<String> map1 = new ArrayList<>();
		
		for(int i=0; i<fileinfo.size(); i++) {
			map1.add(fileinfo.get(i).getUname() + ","
					+ fileinfo.get(i).getFilename() + ","
					+ fileinfo.get(i).getOriginal_filename() + "," 
					+ fileinfo.get(i).getTime() + "/");
		}
		
		String map = String.join("", map1);
		
		model.addAttribute("filelist", map);
		
//		HashMap<String, String> map = new HashMap<String, String>();
//		
//		for(int i=0; i<fileinfo.size(); i++) {
//			System.out.println("ffffffffff " + fileinfo.get(i).getOriginal_filename());
//			map.put(fileinfo.get(i).getFilename(), fileinfo.get(i).getOriginal_filename());
//		}
//		
//		model.addAttribute("filelist", map);
//		
		return "2";
	}
	
//	@GetMapping("/download")
//	public void download(HttpServletResponse response) throws IOException {
//		String path1 = "/uploadfile/";
//		String path = application.getRealPath(path1);
//		
//		byte[] fileByte = FileUtils.readFileToByteArray(new File(path));
//		
//	    response.setContentType("application/octet-stream");
//	    response.setHeader("Content-Disposition", "attachment; fileName=\"" + URLEncoder.encode("tistory.png", "UTF-8")+"\";");
//	    response.setHeader("Content-Transfer-Encoding", "binary");
//
//	    response.getOutputStream().write(fileByte);
//	    response.getOutputStream().flush();
//	    response.getOutputStream().close();
//	}
	
    @PostMapping(value = "/download")
    public ResponseEntity<Object> download(FileuploadForm2 form, HttpServletRequest request) throws IOException {
    	String fileName = form.getFilename();
    	System.out.println("dddddddddddd" + fileName);
    	
    	String path1 = "/uploadfile/";
    	String path = application.getRealPath(path1) + fileName;
    	
    	System.out.println("pppppppppppp" + path);
		try {
			Path filePath = Paths.get(path);
			Resource resource = new InputStreamResource(Files.newInputStream(filePath)); // 파일 resource 얻기
			
			File file = new File(path);
			
			HttpHeaders headers = new HttpHeaders();
			headers.setContentDisposition(ContentDisposition.builder("attachment").filename(file.getName()).build());  // 다운로드 되거나 로컬에 저장되는 용도로 쓰이는지를 알려주는 헤더
			
			return new ResponseEntity<Object>(resource, headers, HttpStatus.OK);
		} catch(Exception e) {
			return new ResponseEntity<Object>(null, HttpStatus.CONFLICT);
		}

    }
	
}

