--
-- Database: `findbugs`
--

-- --------------------------------------------------------

--
-- Table structure for table `findbugsIssues`
--

CREATE TABLE `findbugsIssues` (
  `id` int(11) NOT NULL auto_increment,
  `status` varchar(16) NOT NULL default '',
  `firstSeen` date NOT NULL,
  `lastSeen` date NOT NULL,
  `updated` date NOT NULL,
  `who` varchar(16) NOT NULL default '',
  `comment` text  NOT NULL default '',
  `hash` varchar(32) NOT NULL,
  `bugPattern` varchar(80) NOT NULL,
  `priority` int(11) NOT NULL,
  `primaryClass` varchar(512) NOT NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `hash` (`hash`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=1514 ;

