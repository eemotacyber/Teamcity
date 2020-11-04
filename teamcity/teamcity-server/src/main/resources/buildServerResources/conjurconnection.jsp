
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include-internal.jsp" %>

<jsp:useBean id="keys" class="com.cyberark."/>


<style type="text/css">
    .auth-container {
        display: none;
    }
</style> --%>


    <tr>
        <td><label for="${keys.namespace}">Vault URL:</label></td>
        <td>
            <props:textProperty name="${keys.namespace}"
                                className="longField textProperty_max-width js_max-width"/>
            <span class="error" id="error_${keys.namespace}"/>

        </td>
    </tr>