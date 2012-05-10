<!-- normally you make one big form for the whole plugin-->
<form method="post" enctype="multipart/form-data" name="${model.name}" action="">
	<!--needed in every form: to redirect the request to the right screen-->
	<input type="hidden" name="__target" value="${model.name}">
	<!--needed in every form: to define the action. This can be set by the submit button-->
	<input type="hidden" name="__action">
	
<!-- this shows a title and border -->
	<div class="formscreen">
		<div class="form_header" id="${model.getName()}">
		${model.label}
		</div>
		
		<#--optional: mechanism to show messages-->
		<#list model.getMessages() as message>
			<#if message.success>
		<p class="successmessage">${message.text}</p>
			<#else>
		<p class="errormessage">${message.text}</p>
			</#if>
		</#list>
		
		<div class="screenbody">
			<div class="screenpadding">	
			
<#--begin your plugin-->	
<#--p>The currently selected date: ${model.date?date}</p-->
<#--p>text ${model.getHelloWorld()} </p-->
<#--@action name="resetDatabase" label="reset database (all data will be deleted!!!)"/-->
<input type="submit" value="Drop and create database!" onclick="__action.value='resetDatabase';return true;"/>
<input type="submit" value="Quickly Empty Compute tables!" onclick="__action.value='emptyCompute';return true;"/>
<#--label>Change date:</label><@date name="date" value=model.date/> <@action name="updateDate"/-->
	
<#--end of your plugin-->	
			</div>
		</div>
	</div>
</form>