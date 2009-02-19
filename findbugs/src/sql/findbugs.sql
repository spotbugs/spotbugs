SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

--
-- Database: `findbugs`
--

-- --------------------------------------------------------

--
-- Table structure for table `findbugsIssues`
--

CREATE TABLE `findbugsIssues` (
  `id` int(11) NOT NULL auto_increment,
  `status` varchar(16) NOT NULL,
  `firstSeen` date NOT NULL,
  `updated` date NOT NULL,
  `who` varchar(16) NOT NULL,
  `comment` varchar(160) NOT NULL,
  `hash` varchar(32) NOT NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `hash` (`hash`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;
