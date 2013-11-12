/*
MySQL Backup
Source Server Version: 5.5.32
Source Database: sms
Date: 11/12/2013 22:44:15
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
--  Table structure for `categories`
-- ----------------------------
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parentid` int(11) NOT NULL DEFAULT '0',
  `name` varchar(255) CHARACTER SET utf8 NOT NULL,
  `displayname` varchar(255) CHARACTER SET utf8 NOT NULL,
  `type` varchar(255) CHARACTER SET utf8 NOT NULL,
  `description` text CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `parentid_2` (`parentid`,`name`),
  KEY `parentid` (`parentid`)
) ENGINE=InnoDB AUTO_INCREMENT=93 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- ----------------------------
--  Table structure for `files`
-- ----------------------------
DROP TABLE IF EXISTS `files`;
CREATE TABLE `files` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created_at` bigint(20) NOT NULL,
  `modified_at` bigint(20) NOT NULL,
  `serverid` int(11) NOT NULL,
  `servercategoryid` int(11) NOT NULL,
  `categoryid` int(11) NOT NULL,
  `flag` int(11) NOT NULL,
  `duplicate` int(1) NOT NULL DEFAULT '0',
  `name` varchar(255) CHARACTER SET utf8 NOT NULL,
  `displayname` varchar(255) CHARACTER SET utf8 NOT NULL,
  `directory` varchar(255) CHARACTER SET utf8 NOT NULL,
  `displaydirectory` varchar(255) CHARACTER SET utf8 NOT NULL,
  `type` varchar(255) CHARACTER SET utf8 NOT NULL,
  `extension` varchar(255) CHARACTER SET utf8 NOT NULL,
  `path` text CHARACTER SET utf8 NOT NULL,
  `serverpath` text CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`id`),
  KEY `servercategoryid` (`servercategoryid`),
  KEY `serverid` (`serverid`),
  KEY `categoryid` (`categoryid`)
) ENGINE=InnoDB AUTO_INCREMENT=28805098 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- ----------------------------
--  Table structure for `history`
-- ----------------------------
DROP TABLE IF EXISTS `history`;
CREATE TABLE `history` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `time` bigint(20) NOT NULL,
  `level` varchar(255) CHARACTER SET utf8 NOT NULL,
  `type` varchar(255) CHARACTER SET utf8 NOT NULL,
  `action` varchar(255) CHARACTER SET utf8 NOT NULL,
  `item` text CHARACTER SET utf8 NOT NULL,
  `serverid` int(10) NOT NULL,
  `categoryid` int(10) NOT NULL,
  `servercategoryid` int(10) NOT NULL,
  `fileid` int(10) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=69599 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- ----------------------------
--  Table structure for `servercategories`
-- ----------------------------
DROP TABLE IF EXISTS `servercategories`;
CREATE TABLE `servercategories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `categoryid` int(11) NOT NULL,
  `serverid` int(11) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 NOT NULL,
  `path` text CHARACTER SET utf8 NOT NULL,
  `dontscan` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `categoryid` (`categoryid`,`serverid`)
) ENGINE=InnoDB AUTO_INCREMENT=137 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- ----------------------------
--  Table structure for `servers`
-- ----------------------------
DROP TABLE IF EXISTS `servers`;
CREATE TABLE `servers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8 NOT NULL,
  `displayname` varchar(255) CHARACTER SET utf8 NOT NULL,
  `code` varchar(4) CHARACTER SET utf8 NOT NULL,
  `description` text CHARACTER SET utf8 NOT NULL,
  `type` varchar(255) CHARACTER SET utf8 NOT NULL,
  `config` text CHARACTER SET utf8 NOT NULL,
  `status` varchar(255) CHARACTER SET utf8 NOT NULL,
  `disconnectcount` int(10) NOT NULL DEFAULT '0',
  `disconnected` int(1) NOT NULL DEFAULT '0',
  `enabled` int(10) NOT NULL DEFAULT '1',
  `lastupdate` bigint(20) NOT NULL,
  `lastchange` bigint(20) NOT NULL,
  `ip` varchar(255) CHARACTER SET utf8 NOT NULL,
  `filecount` int(11) NOT NULL,
  `categorycount` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- ----------------------------
--  Records 
-- ----------------------------