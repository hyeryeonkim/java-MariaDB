import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
	public static void main(String[] args) {
		App app = new App();
		app.start();
		
		
	}
}

class App {
	private Map<String, Controller> controllers;
	private void initControllers() {
		controllers = new HashMap<>();
		controllers.put("article", new ArticleController());
		
	}
	public void start() {
		initControllers();
		Factory.getDbConnection().connect();
		
		
		
		
		while (true) {
			
			System.out.printf("명령어 : ");
			String command = Factory.getScanner().nextLine().trim();

			if (command.length() == 0) {
				continue;
			} else if (command.equals("exit")) {
				break;
			}

			Request request = new Request(command);

			if (request.isValidRequest() == false) {
				System.out.println("< 다시 입력바랍니다. >");
				continue;
			}

			if (controllers.containsKey(request.getControllerName()) == false) {
				System.out.println("< 다시 입력바랍니다. >");
				continue;
			}
			controllers.get(request.getControllerName()).doAction(request);
		}
		Factory.getScanner().close();
		
		//화이팅!!! 
	}
}

class DBConnection {
	Connection connection;

	public void connect() {
		String url = "jdbc:mysql://localhost:3306/site5?serverTimezone=UTC";
		String user = "root";
		String password = "";
		String driverName = "com.mysql.cj.jdbc.Driver";
		
		try {
			// ① 로드(카카오 택시에 `com.mysql.cj.jdbc.Driver` 라는 실제 택시 드라이버를 등록)
			// 하지만 개발자는 실제로 `com.mysql.cj.jdbc.Driver`를 다룰 일은 없다.
			// 내부적으로 JDBC가 알아서 다 해주기 때문에 우리는 JDBC의 DriverManager 를 통해서 DB와의 연결을 얻으면 된다.
			Class.forName(driverName);

			// ② 연결
			connection = DriverManager.getConnection(url, user, password);
		} catch (ClassNotFoundException e) {
			// `com.mysql.cj.jdbc.Driver` 라는 클래스가 라이브러리로 추가되지 않았다면 오류발생
			System.out.println("[로드 오류]\n" + e.getStackTrace());
		} catch (SQLException e) {
			// DB접속정보가 틀렸다면 오류발생
			System.out.println("[연결 오류]\n" + e.getStackTrace());
		}
		System.out.println("< MariaDB 연결 성공! > ");
	}

	public int selectRowIntValue(String sql) {
		Map<String, Object> row = selectRow(sql);

		for (String key : row.keySet()) {
			Object value = row.get(key);

			if (value instanceof String) {
				return Integer.parseInt((String) value);
			}
			if (value instanceof Long) {
				return (int) (long) value;
			} else {
				return (int) value;
			}
		}

		return -1;
	}

	public String selectRowStringValue(String sql) {
		Map<String, Object> row = selectRow(sql);

		for (String key : row.keySet()) {
			Object value = row.get(key);
			System.out.println(value);
			return value + "";
		}

		return "";
	}

	public boolean selectRowBooleanValue(String sql) {
		int rs = selectRowIntValue(sql);

		return rs == 1;
	}

	public Map<String, Object> selectRow(String sql) {
		List<Map<String, Object>> rows = selectRows(sql);

		if (rows.size() > 0) {
			return rows.get(0);
		}

		return new HashMap<>();
	}

	public List<Map<String, Object>> selectRows(String sql) {
		// SQL을 적는 문서파일
		Statement statement = null;
		ResultSet rs = null;

		List<Map<String, Object>> rows = new ArrayList<>();

		try {
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			// ResultSet 의 MetaData를 가져온다.
			ResultSetMetaData metaData = rs.getMetaData();
			// ResultSet 의 Column의 갯수를 가져온다.
			int columnSize = metaData.getColumnCount();

			// rs의 내용을 돌려준다.
			while (rs.next()) {
				// 내부에서 map을 초기화
				Map<String, Object> row = new HashMap<>();

				for (int columnIndex = 0; columnIndex < columnSize; columnIndex++) {
					String columnName = metaData.getColumnName(columnIndex + 1);
					Object value = rs.getObject(columnName);
					if ( value instanceof Long ) {
						int numValue = (int)(long)value; //Object형
						row.put(columnName, numValue);
					}
					else if ( value instanceof Timestamp ) {
						String dateValue = value.toString();
						dateValue = dateValue.substring(0, dateValue.length() - 2 );
						row.put(columnName, dateValue);
					}
					else {
						row.put(columnName, columnName);
					}
					// map에 값을 입력 map.put(columnName, columnName으로 getString)
					
				}
				// list에 저장
				rows.add(row);
			}
		} catch (SQLException e) {
			System.err.printf("[SELECT 쿼리 오류, %s]\n" + e.getStackTrace() + "\n", sql);
		}

		try {
			if (statement != null) {
				statement.close();
			}

			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			System.err.println("[SELECT 종료 오류]\n" + e.getStackTrace());
		}

		return rows;
	}

	public int update(String sql) {
		// UPDATE 명령으로 몇개의 데이터가 수정되었는지
		int affectedRows = 0;

		// SQL을 적는 문서파일
		Statement statement = null;

		try {
			statement = connection.createStatement();
			affectedRows = statement.executeUpdate(sql);
		} catch (SQLException e) {
			System.err.printf("[UPDATE 쿼리 오류, %s]\n" + e.getStackTrace() + "\n", sql);
		}

		try {
			if (statement != null) {
				statement.close();
			}
		} catch (SQLException e) {
			System.err.println("[UPDATE 종료 오류]\n" + e.getStackTrace());
		}

		return affectedRows;
	}

	public int insert(String sql) {
		int id = -1;

		// SQL을 적는 문서파일
		Statement statement = null;
		// SQL의 실행결과 보고서
		ResultSet rs = null;

		try {
			statement = connection.createStatement();
			statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
			rs = statement.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getInt(1);
			}
		} catch (SQLException e) {
			System.err.printf("[INSERT 쿼리 오류, %s]\n" + e.getStackTrace() + "\n", sql);
		}

		try {
			if (statement != null) {
				statement.close();
			}

			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			System.err.println("[INSERT 종료 오류]\n" + e.getStackTrace());
		}

		return id;
	}

	public void close() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			System.err.println("[닫기 오류]\n" + e.getStackTrace());
		}
	}
}
class Session {
	private Board currentBoard;

	public Board getCurrentBoard() {
		return currentBoard;
	}

	public void setCurrentBoard(Board currentBoard) {
		this.currentBoard = currentBoard;
	}
	
}
abstract class Controller {
	
	abstract void doAction(Request request);
}

class ArticleController extends Controller {
	private ArticleService articleService;
	ArticleController() {
		articleService = Factory.getArticleService();
	}
	public void doAction(Request request) {
		if (request.getActionName().equals("write")) {
			actionArticleWrite(request);
		} else if ( request.getActionName().equals("list")) {
			actionArticleList(request);
		} else if ( request.getActionName().equals("modify")) {
			actionArticleModify(request);
		} else if ( request.getActionName().equals("delete")) {
			actionArticleDelete(request);
		} else if ( request.getActionName().equals("detail")) {
			actionArticleDetail(request);
		}
		
	}
	private void actionArticleDetail(Request request) {
		int articleId = Integer.parseInt(request.getArg1());
		System.out.println("\t\t== 게시물 상세보기 ==\n");
		List<Map<String, Object>> rows = articleService.getSelectRows("SELECT * FROM article WHERE id = " + articleId );
		if ( rows.isEmpty()) {  // 오........ 좋은디...... 
			System.out.println("존재하지 않는 게시물 입니다.");
			return;
		}
		System.out.println(rows);
		System.out.println("번호  |           날짜                 |            제목              |       내용  ");
		for ( Map<String, Object> map : rows ) {
			System.out.printf( " %-2d | %-10s  | %-15s               | %-15s  \n", map.get("id"), map.get("regDate"), map.get("title"), map.get("body"));
		}
	}  
	private void actionArticleDelete(Request request) {
		System.out.println("== 게시물 삭제 ==");
		int articleId = Integer.parseInt(request.getArg1());
		int id = articleService.getArticleInsert("DELETE FROM article WHERE id = " + articleId );
		System.out.println("== 게시물 삭제 완료 ==");		
	}
	private void actionArticleModify(Request request) {
		int articleId = Integer.parseInt(request.getArg1());
		List<Map<String, Object>> rows = articleService.getSelectRows("SELECT * FROM article WHERE id = " + articleId );
		if ( rows.isEmpty()) {
			System.out.println("존재하지 않는 게시물 입니다.");
			return;
		}
		String title;
		String body;
		System.out.println("== 게시물 수정 ==");
		System.out.printf("제목 : ");
		title = Factory.getScanner().nextLine().trim();
		System.out.printf("내용 : ");
		body = Factory.getScanner().nextLine().trim();
		int id = articleService.getArticleInsert("UPDATE article SET title = '" + title + "', `body` = '" + body + "' WHERE id = " + articleId  );
	}
	private void actionArticleList(Request request) {
		System.out.println("\t\t== 게시물 리스트 ==\n");
		List<Map<String, Object>> rows = articleService.getSelectRows("SELECT * FROM article ORDER BY id DESC");
		System.out.println("번호  |           날짜                 |            제목           ");
		for ( Map<String, Object> map : rows ) {
			System.out.printf( " %-2d | %-10s  | %-15s    \n", map.get("id"), map.get("regDate"), map.get("title"));
		}
	}
	private void actionArticleWrite(Request request) {
		String title;
		String body;
		System.out.println("== 게시물 작성 ==");
		System.out.printf("제목 :");
		title = Factory.getScanner().nextLine().trim();
		System.out.printf("내용 : ");
		body = Factory.getScanner().nextLine().trim();
		int newId = articleService.getArticleInsert("INSERT INTO article SET title = '" + title + "', regDate = NOW(), `body` = '" + body +"'" );
		
	}
	private void actionBoardChange(Request request) {
		
	}
}
class ArticleDao extends Dto {
	private DBConnection dbConnection;
	ArticleDao() {
		dbConnection = Factory.getDbConnection();
	}
	public void boardChange(String boardCode) {
		
	}
	public List<Map<String, Object>> getSelectRows(String sql) {
		return dbConnection.selectRows(sql);
	}
	public String selectRowStringValue(String sql) {
		return dbConnection.selectRowStringValue(sql);
	}
	public int getArticleInsert(String sql) {
		return dbConnection.insert(sql);
	}
	
}
class ArticleService {
	private ArticleDao articleDao;
	ArticleService() {
		articleDao = Factory.getArticleDao();
	}
	public int getArticleInsert(String sql) {
		return articleDao.getArticleInsert(sql);
	}
	public String selectRowStringValue(String sql) {
		return articleDao.selectRowStringValue(sql);
	}
	public List<Map<String, Object>> getSelectRows(String sql) {
		return articleDao.getSelectRows(sql);
	}
	public void boardChange(String boardCode) {
		articleDao.boardChange(boardCode);
	}
	
}
class Request {
	private String requestStr;
	private String controllerName;
	private String actionName;
	private String arg1;
	private String arg2;
	private String arg3;
	
	Request(String requestStr) {
		this.requestStr = requestStr;
		String[] requestStrBits = requestStr.split(" ");
		this.controllerName = requestStrBits[0];

		if (requestStrBits.length > 1) {
			this.actionName = requestStrBits[1];
		}

		if (requestStrBits.length > 2) {
			this.arg1 = requestStrBits[2];
		}

		if (requestStrBits.length > 3) {
			this.arg2 = requestStrBits[3];
		}

		if (requestStrBits.length > 4) {
			this.arg3 = requestStrBits[4];
		}
	}

	public String getControllerName() {
		return controllerName;
	}

	public void setControllerName(String controllerName) {
		this.controllerName = controllerName;
	}

	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public String getArg1() {
		return arg1;
	}

	public void setArg1(String arg1) {
		this.arg1 = arg1;
	}

	public String getArg2() {
		return arg2;
	}

	public void setArg2(String arg2) {
		this.arg2 = arg2;
	}

	public String getArg3() {
		return arg3;
	}

	public void setArg3(String arg3) {
		this.arg3 = arg3;
	}
	
	boolean isValidRequest() {
		return actionName != null;
	}
}
//공유하는 모든 객체리모콘을 보관하는 클래스
class Factory {
	static ArticleService articleService;
	static Session session;
	static DBConnection dbConn;
	static Scanner scanner;
	static ArticleDao articleDao;
	public static ArticleDao getArticleDao() {
		if ( articleDao == null ) {
			articleDao = new ArticleDao();
		}
		return articleDao;
	}
	public static Scanner getScanner() {
		if ( scanner == null  ) {
			scanner = new Scanner(System.in);
		}
		return scanner;
	}
	public static DBConnection getDbConnection() {
		if ( dbConn == null ) {
			dbConn = new DBConnection();
		}
		return dbConn;
	}
	public static Session getSession() {
		if ( session == null ) {
			session = new Session();
		}
		return session;
	}
	public static ArticleService getArticleService() {
		if ( articleService == null ) {
			articleService = new ArticleService();
		}
		return articleService;
	}
}
abstract class Dto {
	private int id;
	private String regDate;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getRegDate() {
		return regDate;
	}
	public void setRegDate(String regDate) {
		this.regDate = regDate;
	}
	Dto() {
		this(0);
	}
	Dto(int id) {
		this(0, Util.getNowDateStr());
	}
	Dto(int id, String regDate) {
		this.id = id;
		this.regDate = regDate;
	}
}
class Board extends Dto {
	private String name;
	private String code;
	public Board(String name, String code) {
		this.name = name;
		this.code = code;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
}
class Util {
	public static String getNowDateStr() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat Date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateStr = Date.format(cal.getTime());
		return dateStr;
	}
}