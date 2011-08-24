<jsp:useBean id="jspHelper" scope="application" class="edu.umd.cs.findbugs.flybush.JspHelper"/>
<%@ page isELIgnored="false" %>

<html>
<head>
    <title>${jspHelper.cloudName} Sign-in</title>

    <!-- Simple OpenID Selector -->
    <link rel="stylesheet" href="/css/openid.css"/>
    <script type="text/javascript" src="/js/jquery-1.2.6.min.js"></script>
    <script type="text/javascript" src="/js/openid-jquery.js"></script>
    <script type="text/javascript">
        $(document).ready(function() {
            openid.img_path = "/images/"
            openid.init('openid_identifier');
        });
    </script>
    <!-- /Simple OpenID Selector -->
</head>
<body>
<table border=0 cellpadding=10>
    <tr>
        <td bgcolor='#b9b9fe' valign='top'>
            <img src='http://findbugs.sourceforge.net/umdFindbugs.png' alt='FindBugs'>
        </td>
        <td valign='top'>
            <div style="color:red;font-size:1.4em">${openid_servlet_filter_msg}</div>

            <form method="POST" id="openid_form">
                <fieldset>
                    <legend>Sign into ${jspHelper.cloudName}</legend>

                    <div id="openid_choice">
                        <p>Please click your account provider:</p>

                        <div id="openid_btns"></div>
                    </div>

                    <div id="openid_input_area">
                    </div>
                    <noscript>
                        <p>OpenID is service that allows you to log-on to many different websites using a single
                            indentity.
                            Find out <a href="http://openid.net/what/">more about OpenID</a> and <a
                                    href="http://openid.net/get/">how
                                to get an OpenID enabled account</a>.</p>
                    </noscript>
                </fieldset>
            </form>
        </td>
    </tr>
</table>
</body>
</html>