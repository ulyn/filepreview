<!DOCTYPE html>

<html lang="en">
<head>
    <style type="text/css">
        body{
            margin: 0;
            padding:0;
            border:0;
        }
    </style>
</head>
<body>
    <#if pdfUrl?contains("http://")>
        <#assign finalUrl="${pdfUrl}">
    <#else>
        <#assign finalUrl="${baseUrl}${pdfUrl}">
    </#if>
 <#setting url_escaping_charset='utf-8'>
<iframe src="/pdfjs/web/viewer.html?file=${finalUrl?url}" width="100%" frameborder="0"></iframe>
</body>
<script type="text/javascript">
    document.getElementsByTagName('iframe')[0].height = document.documentElement.clientHeight-10;
    /**
     * 页面变化调整高度
     */
    window.onresize = function(){
        var fm = document.getElementsByTagName("iframe")[0];
        fm.height = window.document.documentElement.clientHeight-10;
    }
</script>
</html>