package com.example.chatproj.chatproj.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.example.chatproj.chatproj.domain.Chatlog_Table;
import com.example.chatproj.chatproj.domain.Chatroom_Table;
import com.example.chatproj.chatproj.domain.UC_Table;
import com.example.chatproj.chatproj.domain.User;
import com.example.chatproj.chatproj.repository.ChatRepository;
import com.example.chatproj.chatproj.repository.UserRepository;

@Transactional
public class ChatService {
	
		private ChatRepository chatRepository;

		public ChatService(ChatRepository chatRepository) {
			this.chatRepository = chatRepository;
		}

		// chat table 등록
		public Optional<Chatroom_Table> join() {
            return chatRepository.findByCNum();
        }
		
		public void insChatTable(Chatroom_Table user1) {
			chatRepository.save(user1);			
		}
		
		public void insUCTable(UC_Table user2) {
			chatRepository.insUCTable(user2);
		}
		
		public List<UC_Table> getChatList(int sessionNum) {
			List<UC_Table> result = chatRepository.getChatList(sessionNum);
			return result;	
		}
		
		public List<UC_Table> getstringToinfo(int submitListPK, String submitListName){
			List<UC_Table> result = chatRepository.getstringToinfo(submitListPK, submitListName);
			return result;
		}
		
		public List<UC_Table> getUserInfo(int cnumPK){
			List<UC_Table> result = chatRepository.getUserInfo(cnumPK);
			return result;
		}
		
		public void logjoin(Chatlog_Table chatlog_table) {
			chatRepository.save(chatlog_table);			
		}
		
		public List<Chatlog_Table> getChatLog(int cnumPK){
			List<Chatlog_Table> result = chatRepository.getChatLog(cnumPK);
			return result;
		}
		
		public Optional<Chatroom_Table> getChatName(int cnumPK){
			Optional<Chatroom_Table> result = chatRepository.getChatName(cnumPK);
			return result;
		}
		
		public void exitUser(int cnumPK, int sessionNum) {
			chatRepository.exitUser(cnumPK, sessionNum);
		}
		
		public void deleteChatRoom(int cnumPK) {
			chatRepository.deleteChatRoom(cnumPK);
		}

		
}
