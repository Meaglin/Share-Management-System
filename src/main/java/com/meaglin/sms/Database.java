package com.meaglin.sms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.meaglin.sms.model.Server;
import com.meaglin.sms.model.ServerFile;

public class Database {

	private final String url, username, password;

	protected Connection db;

	public Database(Properties config) {
		url = "jdbc:mysql://" + config.getProperty("mysql.host", "localhost")
				+ ":" + config.getProperty("mysql.port", "3306")
				+ "/" + config.getProperty("mysql.database", "sms") + "?useUnicode=true&characterEncoding=utf-8";
		username = config.getProperty("mysql.username", "root");
		password = config.getProperty("mysql.password", "");
		try {
			connect();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection(url, username, password);
	}

	protected void checkConnection() throws SQLException {
		if (!this.db.isValid(0)) {
			this.connect();
		}
	}

	protected final void connect() throws SQLException {
		db = getConnection();
	}

	protected void bindParams(PreparedStatement stmt, Object... params)
			throws SQLException {
		for (int i = 1; i <= params.length; i++) {
			Object param = params[i - 1];
			stmt.setObject(i, param);
		}
	}

	public ResultSet selectQuery(String sql, Object... params)
			throws SQLException {
		this.checkConnection();

		PreparedStatement stmt = this.db.prepareStatement(sql);

		if (params != null) {
			this.bindParams(stmt, params);
		}

		return stmt.executeQuery();
	}

	public Object selectQueryOne(String sql, Object fallback, Object... params) {
		try {
			this.checkConnection();

			ResultSet result = this.selectQuery(sql, params);

			if (!result.next()) {
				return fallback;
			}

			return result.getObject(1);

		} catch (SQLException e) {
		}

		return fallback;
	}

	public void updateQuery(String sql, Object... params) throws SQLException {
		this.checkConnection();

		PreparedStatement stmt = this.db.prepareStatement(sql);

		if (params != null) {
			this.bindParams(stmt, params);
		}

		stmt.executeUpdate();
		stmt.close();
	}

	public void insert(String table, String[] fields, List<Object[]> rows)
			throws SQLException {
		this.checkConnection();

		String[] fieldPlaceholders = new String[fields.length];
		Arrays.fill(fieldPlaceholders, "?");
		String sql = "INSERT INTO `" + table + "` (`" + implode(fields, "`, `")
				+ "`) VALUES (" + implode(fieldPlaceholders, ", ") + ");";

		PreparedStatement stmt = this.db.prepareStatement(sql);

		this.db.setAutoCommit(false);
		for (Object[] params : rows) {
			this.bindParams(stmt, params);
			stmt.addBatch();
		}
		stmt.executeBatch();
		this.db.commit();
		this.db.setAutoCommit(true);
		this.collect(null, stmt, null);
		// stmt.close();
	}

	public void insertOne(String table, String[] fields, Object... params)
			throws SQLException {
		this.checkConnection();

		String[] fieldPlaceholders = new String[fields.length];
		Arrays.fill(fieldPlaceholders, "?");
		String sql = "INSERT INTO `" + table + "` (`" + implode(fields, "`, `")
				+ "`) VALUES (" + implode(fieldPlaceholders, ", ") + ");";

		PreparedStatement stmt = this.db.prepareStatement(sql);
		this.bindParams(stmt, params);
		stmt.execute();

		this.collect(null, stmt, null);
	}

	public static String implode(String[] array, String separator) {
		if (array.length == 0) {
			return "";
		}

		StringBuilder buffer = new StringBuilder();

		for (String str : array) {
			buffer.append(separator);
			buffer.append(str);
		}

		return buffer.substring(separator.length()).trim();
	}

	private static final String UPDATE_SERVER = "UPDATE servers SET status = ?, lastupdate = ?, lastchange = ?, filecount = ?, categorycount = ?, disconnected = ? WHERE id = ?";
	private static final String UPDATE_FILES = "UPDATE files SET flag = ?, duplicate = ? WHERE id = ?";
	private static final String DELETE_FILES = "DELETE FROM files WHERE id = ?";

	public void save(Server server) {
		try {
			updateQuery(UPDATE_SERVER, server.getStatus(),
					server.getLastupdate(), server.getLastchange(),
					server.getFilecount(), server.getCategorycount(),
					server.isDisconnected(), server.getId());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void save(List<ServerFile> files) {
		List<Object[]> params = new ArrayList<>();
		try {

			this.checkConnection();
			PreparedStatement updatestmt = this.db
					.prepareStatement(UPDATE_FILES);

			this.db.setAutoCommit(false);
			for (ServerFile file : files) {
				if (file.getId() != 0) {
					this.bindParams(updatestmt, file.getFlag(), file.isDuplicate(), file.getId());
					updatestmt.addBatch();
				} else {
					params.add(new Object[] { file.getServerid(),
							file.getServercategoryid(), file.getCategoryid(),
							file.getName(), file.getDisplayname(),
							file.getDirectory(), file.getDisplaydirectory(),
							file.getType(), file.getFlag(), file.isDuplicate(),
							file.getExtension(), file.getPath(),
							file.getServerpath() });
				}
			}

			updatestmt.executeBatch();
			this.db.commit();
			this.db.setAutoCommit(true);
			updatestmt.close();

			insert("files", new String[] { "serverid", "servercategoryid",
					"categoryid", "name", "displayname", "directory",
					"displaydirectory", "type", "flag", "duplicate", "extension", "path",
					"serverpath" }, params);
		} catch (Exception ex) {

		}

	}

	public void delete(List<ServerFile> toDelete) {
		try {
			this.checkConnection();
			PreparedStatement updatestmt = this.db
					.prepareStatement(DELETE_FILES);

			this.db.setAutoCommit(false);
			for (ServerFile file : toDelete) {
				this.bindParams(updatestmt, file.getId());
				updatestmt.addBatch();
			}

			updatestmt.executeBatch();
			this.db.commit();
			this.db.setAutoCommit(true);
			updatestmt.close();
		} catch (Exception ex) {

		}
	}
	
	public void cleanFiles() {
		try {
	        this.updateQuery("DELETE FROM `files`");
        } catch (SQLException e) {
	        e.printStackTrace();
        }
	}
	

	// Close all connections and resources.
	public void collect(Connection conn, PreparedStatement st, ResultSet res) {
		if (res != null) {
			try {
				res.close();
			} catch (SQLException e) {
			}
		}
		if (st != null) {
			try {
				st.close();
			} catch (SQLException e) {
			}
		}
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
			}
		}
	}

}
