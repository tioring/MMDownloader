package ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import common.DownloadMod;
import common.ErrorHandling;
import common.InputCheck;
import downloader.Preprocess;
import sys.SystemInfo;
import sys.Configuration;

public class UI implements DownloadMod {
	private final int EXIT = 0;
	
	private UI(){
		SystemInfo.makeDir(); //시작과 동시에 디폴트 폴더 생성.
		SystemInfo.makeDir(SystemInfo.PATH); //시작과 동시에 사용자 지정 다운로드 폴더 생성
		Configuration.init(); //설정파일(MMDownloader.properties 읽기 & 적용 & 저장) 수행

		SystemInfo.printProgramInfo();//버전 출력
	}
	
	/* Double Checking Locking Singleton */
	private static volatile UI instance = null;
	public static UI getInstance(){
		if(instance == null) {
			synchronized(UI.class) {
				if(instance == null) instance = new UI();
			}
		}
		return instance;
	}

	public void showMenu() throws Exception {
		final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		Preprocess preprocess = Preprocess.getInstance();
		String comicAddress, input;
		int menuNum = Integer.MAX_VALUE;

		while (menuNum != EXIT) {
			printMenu(); //메뉴 출력
			input = in.readLine();
			if(InputCheck.isValid(input, InputCheck.ONLY_NUMBER) == false) {
				ErrorHandling.printError("잘못된 입력값입니다.", false);
				continue;
			}
			menuNum = Integer.parseInt(input);

			switch (menuNum) {
			case 1: //일반 다운로드
				System.out.print("주소를 입력하세요: ");
				
				comicAddress = in.readLine().trim();
				if(InputCheck.isValid(comicAddress, InputCheck.IS_HTTP_URL) == false) {
					ErrorHandling.printError("잘못된 입력값입니다.", false);
					continue;
				}
				
				preprocess.connector(comicAddress, ALL_DOWNLOAD, in);
				preprocess.close();
				break;

			case 2: //선택적 다운로드
				System.out.print("전체보기 주소를 입력하세요: ");
				
				comicAddress = in.readLine().trim();
				if(InputCheck.isValid(comicAddress, InputCheck.IS_HTTP_URL) == false) {
					ErrorHandling.printError("잘못된 입력값입니다.", false);
					continue;
				}
				
				preprocess.connector(comicAddress, SELECTIVE_DOWNLOAD, in);
				preprocess.close();
				break;
				
			case 3: //저장 폴더 열기
				SystemInfo.makeDir(SystemInfo.PATH);
				SystemInfo.openDir(SystemInfo.PATH);
				break;
			
			case 4: //마루마루 사이트 열기
				SystemInfo.openBrowser();
				break;
				
			case 8: //환경설정
				printSettingMenu();
				
				input = in.readLine().trim();
				if(InputCheck.isValid(input, InputCheck.ONLY_NUMBER) == false) {
					ErrorHandling.printError("잘못된 입력값입니다.", false);
					continue;
				}
				menuNum = Integer.parseInt(input);
				
				/* 환경설정 메뉴 */
				switch(menuNum){
				
				case 1: //업데이트 확인
					SystemInfo.printLatestVersionInfo(in);
					break;
					
				case 2: //저장경로 변경
					changeSavePath(in);
					break;
			
				case 3: //다운받은 만화 하나로 합치기
					mergeImage(in);
					break;
					
				case 4: //디버깅 모드
					debugMode(in);
					break;

				case 5: //멀티스레딩 모드
					multiThreadMode(in);
					break;
				}
				
				menuNum = 8; //이걸 달아줘야지 종료되는거 막을 수 있음
				break;
			
			case 9: //도움말
				SystemInfo.help();
				break;
			
			case 0: //종료
				System.out.println("프로그램을 종료합니다");
				break;
			}
		}
		in.close(); //BufferedReader close
	}
	
	/**
	 * <p>UI에 보여줄 메뉴 출력 메서드
	 */
	private void printMenu(){
		String menu = 
				"메뉴를 선택하세요\n"+
				"  1. 만화 다운로드\n"+
				"  2. 선택적 다운로드\n"+
				"  3. 다운로드 폴더 열기\n"+
				"  4. 마루마루 접속\n"+
				"  8. 환경설정\n"+
				"  9. 도움말\n"+
				"  0. 종료";
		System.out.println(menu);
	}
	
	private void printSettingMenu() {
		String settingMenu = 
				"설정할 메뉴를 선택하세요\n"+
				"  1. 업데이트 확인\n"+
				"  2. 저장경로 변경\n"+
				"  3. 이미지 병합 설정\n"+
				"  4. 디버깅 모드 설정\n"+
				"  5. 멀티스레딩 설정\n"+
				"  9. 뒤로";
		System.out.println(settingMenu);
	}
	
	/**
	 * 메뉴 8-2 저장경로 변경
	 * @param in
	 * @throws Exception
	 */
	private void changeSavePath(final BufferedReader in) throws Exception {
		System.out.printf("현재 저장경로: %s\n변경할 경로를 입력하세요: ", SystemInfo.PATH);
		
		String path = in.readLine().trim();
		File newPath = new File(path);
	
		/* 입력한 경로가 만든 적이 없는 경로 & 그런데 새로 생성 실패 */
		if(newPath.exists()==false && newPath.mkdirs()==false) {
			ErrorHandling.printError("저장경로 변경 실패", false);
			return;
		}
		
		/* 생성 가능한 정상적인 경로라면 */
		Configuration.setProperty("PATH", path);
		Configuration.refresh(); //store -> load - > apply
		System.out.println("저장경로 변경 완료!");
	}
	
	/**
	 * 메뉴 8-3 이미지 합치기
	 * @param in
	 * @throws Exception
	 */
	private void mergeImage(final BufferedReader in) throws Exception {
		String input;
		boolean merge = Configuration.getBoolean("MERGE", false);
		System.out.printf("true면 다운받은 만화를 하나의 긴 파일로 합친 파일을 추가로 생성합니다(현재: %s)\n", merge);
		System.out.print("값 입력(true or false): ");
		
		input = in.readLine().toLowerCase();
		if(!input.equals("true") && !input.equals("false")) {
			ErrorHandling.printError("잘못된 값입니다.", false);
			return;
		}
		
		Configuration.setProperty("MERGE", input);
		Configuration.refresh();
		System.out.println("변경 완료");
	}

	/**
	 * 메뉴 8-4 디버깅 모드
	 * @param in
	 * @throws Exception
	 */
	private void debugMode(final BufferedReader in) throws Exception {
		boolean debug = Configuration.getBoolean("DEBUG", false);
		System.out.printf("true면 다운로드 과정에 파일의 용량과 메모리 사용량이 같이 출력됩니다(현재: %s)\n", debug);
		System.out.print("값 입력(true or false): ");
		
		String input = in.readLine().toLowerCase();
		if(!input.equals("true") && !input.equals("false")) {
			ErrorHandling.printError("잘못된 값입니다.", false);
			return;
		}
		
		Configuration.setProperty("DEBUG", input);
		Configuration.refresh();
		System.out.println("변경 완료");
	}
	
	/**
	 * 메뉴 8-5 멀티스레딩 모드
	 * @param in
	 * @throws Exception
	 */
	private void multiThreadMode(final BufferedReader in) throws Exception {
		int multi = Configuration.getInt("MULTI", 2);
		System.out.printf("다운로드에 할당할 스레드 값 설정합니다(현재: %d)\n", multi);
		System.out.println("* 기본 값은 2이며, 대체로 값이 커질수록 성능은 좋아지나 메모리 사용량이 증가합니다.\n"
						+ " 0: 멀티스레딩을 하지 않습니다 (초저성능)\n"
						+ " 1: 코어 개수의 절반 만큼을 할당합니다 (저성능)\n"
						+ " 2: 코어 개수 만큼을 할당합니다 (기본값, 권장)\n"
						+ " 3: 코어 개수의 2배 만큼을 할당합니다 (고성능)\n"
						+ " 4: 사용할 수 있는 최대한 할당합니다 (초고성능)");
		System.out.print("값 입력(0 ~ 4): ");
		
		String input = in.readLine().replaceAll(" ", "");
		if(input.matches("[0-4]") == false) {
			ErrorHandling.printError("잘못된 값입니다.", false);
			return;
		}
		
		Configuration.setProperty("MULTI", input);
		Configuration.refresh();
		System.out.println("변경 완료");
	}
	
	public void close(){
		instance = null;
	}
}
/*
변경사항
1. 오타 수정
2. 입력 부분 정규식 검증 추가
*/