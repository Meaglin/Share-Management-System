SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;


CREATE TABLE IF NOT EXISTS `categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parentid` int(11) NOT NULL DEFAULT '0',
  `name` varchar(255) NOT NULL,
  `displayname` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `description` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `parentid_2` (`parentid`,`name`),
  KEY `parentid` (`parentid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=59 ;

CREATE TABLE IF NOT EXISTS `files` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created_at` bigint(20) NOT NULL,
  `modified_at` bigint(20) NOT NULL,
  `serverid` int(11) NOT NULL,
  `servercategoryid` int(11) NOT NULL,
  `categoryid` int(11) NOT NULL,
  `flag` int(11) NOT NULL,
  `duplicate` int(1) NOT NULL DEFAULT '0',
  `name` varchar(255) NOT NULL,
  `displayname` varchar(255) NOT NULL,
  `directory` varchar(255) NOT NULL,
  `displaydirectory` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `extension` varchar(255) NOT NULL,
  `path` text NOT NULL,
  `serverpath` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `servercategoryid` (`servercategoryid`),
  KEY `serverid` (`serverid`),
  KEY `categoryid` (`categoryid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=140508 ;

CREATE TABLE IF NOT EXISTS `history` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `time` bigint(20) NOT NULL,
  `level` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `action` varchar(255) NOT NULL,
  `serverid` int(11) NOT NULL,
  `servercategoryid` int(11) NOT NULL,
  `categoryid` int(11) NOT NULL,
  `fileid` int(11) NOT NULL,
  `item` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=13 ;

CREATE TABLE IF NOT EXISTS `servercategories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `categoryid` int(11) NOT NULL,
  `serverid` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `path` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `categoryid` (`categoryid`,`serverid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=65 ;

CREATE TABLE IF NOT EXISTS `servers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `displayname` varchar(255) NOT NULL,
  `code` varchar(4) NOT NULL,
  `description` text NOT NULL,
  `type` varchar(255) NOT NULL,
  `config` text NOT NULL,
  `status` varchar(255) NOT NULL,
  `disconnectcount` int(10) NOT NULL DEFAULT '0',
  `disconnected` int(1) NOT NULL DEFAULT '0',
  `enabled` int(10) NOT NULL DEFAULT '1',
  `lastupdate` bigint(20) NOT NULL,
  `lastchange` bigint(20) NOT NULL,
  `ip` varchar(255) NOT NULL,
  `filecount` int(11) NOT NULL,
  `categorycount` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=6 ;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
