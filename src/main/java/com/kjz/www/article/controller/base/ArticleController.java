
package com.kjz.www.article.controller.base;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSSClient;
import com.kjz.www.tags.domain.Tags;
import com.kjz.www.tags.service.ITagsService;
import com.kjz.www.tags.vo.TagsVo;
import com.kjz.www.utils.*;

import org.apache.commons.collections.map.HashedMap;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSON;

import java.math.BigDecimal;

import com.kjz.www.common.WebResponse;
import com.kjz.www.article.service.IArticlePhotoService;
import com.kjz.www.article.service.IArticleService;
import com.kjz.www.article.service.IArticleTagsService;
import com.kjz.www.article.domain.Article;
import com.kjz.www.article.domain.ArticlePhoto;
import com.kjz.www.article.domain.ArticleTags;
import com.kjz.www.article.vo.ArticlePhotoVo;
import com.kjz.www.article.vo.ArticleTagsVo;
import com.kjz.www.article.vo.ArticleVo;
import com.kjz.www.article.vo.ArticleVoFont;
import com.kjz.www.utils.vo.UserCookie;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

@Controller
@RequestMapping("/article")
public class ArticleController {

    @Autowired
    protected WebResponse webResponse;

    @Resource
    protected UserUtils userUtils;

    @Resource
    protected ArticleTagsUtils articleTagsUtils;
    
    @Resource
    protected ArticlePhotoUtils articlePhotoUtils;
    
    @Resource
    protected FilterHtmlUtil filterHtmlUtil;

    @Resource
    protected SensitivewordFilterUtils sensitivewordFilterUtils;

    @Resource
    protected IArticleService articleService;

    @Resource
    protected IArticlePhotoService articlePhotoService;
    
    @Resource
    protected IArticleTagsService articleTagsService;
    
    @Resource
    protected ITagsService tagsService;
    //判断添加或修改
    @RequestMapping(value = "/addOrEditArticle", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse addOrEditArticle(HttpServletRequest request, HttpServletResponse response, HttpSession session, String articleId, @RequestParam(required = false) String userId, @RequestParam(required = false) String title, @RequestParam(required = false) String content, @RequestParam(required = false) String preContent, @RequestParam(required = false) String clicks, @RequestParam(required = false) String typeName, @RequestParam(required = false) String isPass, @RequestParam(required = false) String tbStatus,@RequestParam(required = false)String tagsId) {
        if (articleId == null || articleId.length() == 0) {
            return this.addArticle(request, response, session, userId, title, content, preContent, clicks, typeName, isPass, tagsId);
        } else {
            return this.editArticle(request, response, session, articleId, userId, title, content, preContent, clicks, typeName, isPass, tbStatus,tagsId);
        }
    }

    //添加文章
    @RequestMapping(value = "/addArticle", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse addArticle(HttpServletRequest request, HttpServletResponse response, HttpSession session, String userId, String title, String content, String preContent, String clicks, String typeName, String isPass, String tagsId) {
        //初始化数据
        Object data = null;
        String statusMsg = "";
        Integer statusCode = 200;
        Map<String, String> paramMap = new HashMap<String, String>();//保存前端传过来的Json到Map集合
        paramMap.put("userId", userId);
        paramMap.put("title", title);
        paramMap.put("content", content);
        paramMap.put("preContent", preContent);
        paramMap.put("clicks", clicks);
        paramMap.put("typeName", typeName);
        paramMap.put("isPass", isPass);
        data = paramMap;//map集合保存到data
        //数据校验
        if (userId == null || "".equals(userId.trim()) || title == null || "".equals(title.trim()) || content == null || "".equals(content.trim()) || clicks == null || "".equals(clicks.trim()) || typeName == null || "".equals(typeName.trim()) || isPass == null || "".equals(isPass.trim())) {
            statusMsg = " 参数为空错误！！！！";
            statusCode = 201;
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }
//        if (title.length() > 50 || content.length() > 65535 || preContent.length() > 65535 || typeName.length() > 50 || isPass.length() > 50) {
//            statusMsg = " 参数长度过长错误！！！";
//            statusCode = 201;
//            return webResponse.getWebResponse(statusCode, statusMsg, data);
//        }
        String tbStatus = "normal";
        Article article = new Article();
//        //获得用户userCookie
//        UserCookie userCookie = this.userUtils.getLoginUser(request, response, session);
//        if (userCookie == null) {//判断userCookie是否为空
//            statusMsg = "请登录！";
//            statusCode = 201;
//            data = statusMsg;
//            return webResponse.getWebResponse(statusCode, statusMsg, data);//返回WebResponse对象
//        }
//        //小黑屋:若用户状态为“禁止”，不允许发表文章
//        else{
//            String userTbStatus=this.userUtils.getUserFromSession(request, response, session).getTbStatus();
//            if(userTbStatus.equals("禁止")){
//                statusMsg = "您已进入黑名单，无法发表文章";
//                statusCode = 201;
//                data = statusMsg;
//                return webResponse.getWebResponse(statusCode, statusMsg, data);//返回WebResponse对象
//                }
//            else{
//                statusMsg = "添加成功";
//                statusCode = 200;
//                data = statusMsg;
//                //return webResponse.getWebResponse(statusCode, statusMsg, data);//返回WebResponse对象
//            }
//        }

        boolean isAdd = true;
        //交给判断信息
        return this.addOrEditArticle(request, response, session, data, article,userId,title,content,preContent,clicks,typeName,isPass,tbStatus, isAdd,tagsId);
    }

    //更改文章
    @RequestMapping(value = "/editArticle", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse editArticle(HttpServletRequest request, HttpServletResponse response, HttpSession session, String articleId, @RequestParam(required = false) String userId, @RequestParam(required = false) String title, @RequestParam(required = false) String content, @RequestParam(required = false) String preContent, @RequestParam(required = false) String clicks, @RequestParam(required = false) String typeName, @RequestParam(required = false) String isPass, @RequestParam(required = false) String tbStatus,@RequestParam(required = false)String tagsId) {
        Object data = null;
        String statusMsg = "";
        Integer statusCode = 200;
        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("articleId", articleId);
        paramMap.put("userId", userId);
        paramMap.put("title", title);
        paramMap.put("content", content);
        paramMap.put("preContent", preContent);
        paramMap.put("clicks", clicks);
        paramMap.put("typeName", typeName);
        paramMap.put("isPass", isPass);
        paramMap.put("tbStatus", tbStatus);
        data = paramMap;
        if (articleId == null || "".equals(articleId.trim())) {
            statusMsg = "未获得主键参数错误！！！";
            statusCode = 201;
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }
        Integer articleIdNumeri = articleId.matches("^[0-9]*$") ? Integer.parseInt(articleId) : 0;
        if (articleIdNumeri == 0) {
            statusMsg = "主键不为数字错误！！！";
            statusCode = 201;
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }
        ArticleVo articleVo = this.articleService.getById(articleIdNumeri);
        Article article = new Article();
        BeanUtils.copyProperties(articleVo, article);
        UserCookie userCookie = this.userUtils.getLoginUser(request, response, session);
//        if (userCookie == null) {
//            statusMsg = "请登录！";
//            statusCode = 201;
//            data = statusMsg;
//            return webResponse.getWebResponse(statusCode, statusMsg, data);
//        }

        boolean isAdd = false;
        return this.addOrEditArticle(request, response, session, data, article,userId,title,content,preContent,clicks,typeName,isPass,tbStatus, isAdd,tagsId);
    }


    //校验文章格式
    private WebResponse addOrEditArticle(HttpServletRequest request, HttpServletResponse response, HttpSession session, Object data, Article article, String userId, String title, String content, String preContent, String clicks, String typeName, String isPass, String tbStatus, boolean isAdd,String tagsId) {
        String statusMsg = "";
        Integer statusCode = 200;
        Integer userIdNumeri = 0;
        if (userId != null && !("".equals(userId.trim()))) {
            if (!userId.matches("^[0-9]*$")) {
                statusMsg = " 参数数字型错误！！！不能为0,userId";
                statusCode = 201;
                return webResponse.getWebResponse(statusCode, statusMsg, data);
            }
            userIdNumeri = Integer.parseInt(userId);
            article.setUserId(userIdNumeri);
        }
        if (title != null && !("".equals(title.trim()))) {
            if(title.length() > 50) {
                statusMsg = " 参数长度过长错误,title";
                statusCode = 201;
                return webResponse.getWebResponse(statusCode, statusMsg, data);
            }
            article.setTitle(title);
        }
        if (content != null && !("".equals(content.trim()))) {
            if(content.length() > 65535) {
                statusMsg = " 参数长度过长错误,content";
                statusCode = 201;
                return webResponse.getWebResponse(statusCode, statusMsg, data);
            }
            article.setContent(content);
        }
        if (preContent != null && !("".equals(preContent.trim()))) {
            if(preContent.length() > 65535) {
                statusMsg = " 参数长度过长错误,preContent";
                statusCode = 201;
                return webResponse.getWebResponse(statusCode, statusMsg, data);
            }
            article.setPreContent(preContent);
        }
        Integer clicksNumeri = 0;
        if (clicks != null && !("".equals(clicks.trim()))) {
            if (!clicks.matches("^[0-9]*$")) {
                statusMsg = " 参数数字型错误！！！不能为0,clicks";
                statusCode = 201;
                return webResponse.getWebResponse(statusCode, statusMsg, data);
            }
            clicksNumeri = Integer.parseInt(clicks);
            article.setClicks(clicksNumeri);
        }
        if (typeName != null && !("".equals(typeName.trim()))) {
            if(typeName.length() > 50) {
                statusMsg = " 参数长度过长错误,typeName";
                statusCode = 201;
                return webResponse.getWebResponse(statusCode, statusMsg, data);
            }
            article.setTypeName(typeName);
        }
        if (isPass != null && !("".equals(isPass.trim()))) {
            if(isPass.length() > 50) {
                statusMsg = " 参数长度过长错误,isPass";
                statusCode = 201;
                return webResponse.getWebResponse(statusCode, statusMsg, data);
            }
            article.setIsPass(isPass);
        }
        if (tbStatus != null && !("".equals(tbStatus.trim()))) {
            if(tbStatus.length() > 50) {
                statusMsg = " 参数长度过长错误,tbStatus";
                statusCode = 201;
                return webResponse.getWebResponse(statusCode, statusMsg, data);
            }
            article.setTbStatus(tbStatus);
        }
        if (isAdd) {
            this.articleService.insert(article);
            if (article.getArticleId() > 0) {//若文章表插入成功
                //插入文章-标签表
                ArticleTags articleTagsEntity=this.articleTagsUtils.setEntity(article.getArticleId(), tagsId, tbStatus);
                this.articleTagsService.insert(articleTagsEntity);
                statusMsg = "成功插入！！！";
            } else {
                statusCode = 202;
                statusMsg = "insert false";
            }
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }
        int num = this.articleService.update(article);
        if (num > 0) {
            statusMsg = "成功修改！！！";
        } else {
            statusCode = 202;
            statusMsg = "update false";
        }
        return webResponse.getWebResponse(statusCode, statusMsg, data);
    }

    //获取一篇文章（通过articleId）
    @RequestMapping(value = "/getArticleById", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse getArticleById(String articleId) {
        Object data = articleId;
        Integer statusCode = 200;
        String statusMsg = "";
        if (articleId == null || articleId.length() == 0 || articleId.length() > 11) {
            statusMsg = "参数为空或参数过长错误！！！";
            statusCode = 201;
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }
        Integer articleIdNumNumeri = articleId.matches("^[0-9]*$") ? Integer.parseInt(articleId) : 0;
        if (articleIdNumNumeri == 0 ) {
            statusMsg = "参数数字型错误！！！";
            statusCode = 201;
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }
        //通过校验
        Map<Object, Object> datamap = new HashMap<Object, Object>();//data
        
        ArticleVo articleVo = this.articleService.getById(articleIdNumNumeri);
        //有此文章
        if (articleVo != null && articleVo.getArticleId() > 0) {
        	//获得用户昵称
        	String nickname=this.userUtils.getUserById(articleVo.getUserId()).getNickname();
        	articleVo.setClicks(articleVo.getClicks()+1);//浏览数+1
        	//获得文章标签名字
    		LinkedHashMap<String, String> articleTagscondition = new LinkedHashMap<String, String>();
    		articleTagscondition.put("article_id='"+articleIdNumNumeri+"'", "and");
    		  //获得文章标签中间表
    		List<ArticleTagsVo> articleTagsList=this.articleTagsService.getList(articleTagscondition, 1, 10);
    		    //获得标签列表中的tags_name By tags_id
    		List<Tags> tagsList=new LinkedList<Tags>();
    		//遍历中间表对象
    		for(ArticleTagsVo articleTagsvo:articleTagsList){
    			Integer tagsId=articleTagsvo.getTagsId();//tags_id
    			TagsVo tagsvo=this.tagsService.getById(tagsId);
    			Tags tags=new Tags();
    			BeanUtils.copyProperties(tagsvo,tags);
    			tagsList.add(tags);
    		}
    		//json.put("tagsList", tagsList);
    		datamap.put("tagsList", tagsList);
        	datamap.put("nickname", nickname);
            datamap.put("articleVo", articleVo);
            data = datamap;
            statusMsg = "获取单条数据成功！！！";
        } else {
            statusCode = 202;
            statusMsg = "no record!!!";
        }
        return webResponse.getWebResponse(statusCode, statusMsg, data);
    }

    @RequestMapping(value = "/getOneArticle", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse getOneArticle(@RequestParam(defaultValue = "正常", required = false) String tbStatus) {
        LinkedHashMap<String, String> condition = new LinkedHashMap<String, String>();
        condition.put("tb_status='" + tbStatus + "'", "");
        ArticleVo articleVo = this.articleService.getOne(condition);
        Object data = null;
        String statusMsg = "";
        if (articleVo != null && articleVo.getArticleId() > 0) {
            data = articleVo;
            statusMsg = "根据条件获取单条数据成功！！！";
        } else {
            statusMsg = "no record!!!";
        }
        return webResponse.getWebResponse(statusMsg, data);
    }

    @RequestMapping(value = "/getArticleList", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse getArticleList(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                                      @RequestParam(defaultValue = "1", required = false) Integer pageNo,
                                      @RequestParam(defaultValue = "10", required = false) Integer pageSize,
                                      @RequestParam(defaultValue = "正常", required = false) String tbStatus,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "article_id", required = false) String order,
                                      @RequestParam(defaultValue = "desc", required = false) String desc ) {
        Object data = null;
        String statusMsg = "";
        int statusCode = 200;
        LinkedHashMap<String, String> condition = new LinkedHashMap<String, String>();
        if (tbStatus != null && tbStatus.length() > 0) {
            condition.put("tb_status='" + tbStatus + "'", "and");
        }
        if (keyword != null && keyword.length() > 0) {
            StringBuffer buf = new StringBuffer();
            buf.append("(");
            buf.append("title like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("content like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("pre_content like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("type_name like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("is_pass like '%").append(keyword).append("%'");
            buf.append(")");
            condition.put(buf.toString(), "and");
        }
        String field = null;
        if (condition.size() > 0) {
            condition.put(condition.entrySet().iterator().next().getKey(), "");
        }
        int count = this.articleService.getCount(condition, field);
        if (order != null && order.length() > 0 & "desc".equals(desc)) {
            order = order + " desc";
        }
        List<ArticleVo> list = this.articleService.getList(condition, pageNo, pageSize, order, field);
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("total", count);
        int size = list.size();
        if (size > 0) {
            List<ArticleVoFont> listFont = new ArrayList<ArticleVoFont>();
            ArticleVo vo;
            ArticleVoFont voFont = new ArticleVoFont();
            for (int i = 0; i < size; i++) {
                vo = list.get(i);
                BeanUtils.copyProperties(vo, voFont);
                listFont.add(voFont);
                voFont = new ArticleVoFont();
            }
            map.put("list", listFont);
            data = map;
            statusMsg = "根据条件获取分页数据成功！！！";
        } else {
            map.put("list", list);
            data = map;
            statusCode = 202;
            statusMsg = "no record!!!";
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }
        return webResponse.getWebResponse(statusCode, statusMsg, data);
    }

    //所有文章（包括未审核，通过，不通过，状态正常）
    @RequestMapping(value = "/getAdminArticleList", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getAdminArticleList(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                                      @RequestParam(defaultValue = "1", required = false) Integer pageNo,
                                      @RequestParam(defaultValue = "10", required = false) Integer pageSize,
                                      @RequestParam(defaultValue = "正常", required = false) String tbStatus,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "article_id", required = false) String order,
                                      @RequestParam(defaultValue = "desc", required = false) String desc ) {
        Object data = null;
        String statusMsg = "";
        int statusCode = 200;
        LinkedHashMap<String, String> condition = new LinkedHashMap<String, String>();

        if (tbStatus != null && tbStatus.length() > 0) {
            condition.put("tb_status='" + tbStatus + "'", "and");
        }
        //condition.put("is_pass= '通过'","and");
        if (keyword != null && keyword.length() > 0) {
            StringBuffer buf = new StringBuffer();
            buf.append("(");
            buf.append("title like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("content like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("pre_content like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("type_name like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("is_pass like '%").append(keyword).append("%'");
            buf.append(")");
            condition.put(buf.toString(), "and");
        }
        String field = null;
        if (condition.size() > 0) {
            condition.put(condition.entrySet().iterator().next().getKey(), "");
        }
        int count = this.articleService.getCount(condition, field);
        if (order != null && order.length() > 0 & "desc".equals(desc)) {
            order = order + " desc";
        }
        List<ArticleVo> list = this.articleService.getList(condition, pageNo, pageSize, order, field);
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("total", count);
        int size = list.size();
        if (size > 0) {
            map.put("list", list);
            data = map;
            statusMsg = "根据条件获取分页数据成功！！！";
        } else {
            map.put("list", list);
            data = map;
            statusCode = 202;
            statusMsg = "no record!!!";
        }
        return JSON.toJSONString(data);
    }

    //根据文章id删除（数据库不保留
    @RequestMapping(value = "/delArticle", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse delArticle(int id) {
        int num = this.articleService.delBySign(id);;
        Object data = null;
        String statusMsg = "";
        if (num > 0) {
            statusMsg = "成功删除！！！";
        } else {
            statusMsg = "no record!!!";
        }
        return webResponse.getWebResponse(statusMsg, data);
    }

    //发表文章（String userId, String title, String content,  String tagsId,String articlePhotoUrl,String typeName）
    private WebResponse newArticle(HttpServletRequest request, HttpServletResponse response, HttpSession session, String userId, String title, String content,  String tagsId,String articlePhotoUrl,String typeName) {
        //初始化数据
        Object data = null;
        String statusMsg = "";
        Integer statusCode = 200;
        String preContent=this.filterHtmlUtil.filterResult(content);//获得预览
        Map<String, String> paramMap = new HashMap<String, String>();//保存前端传过来的Json到Map集合
        paramMap.put("userId", userId);
        paramMap.put("title", title);
        paramMap.put("content", content);
        data = paramMap;//map集合保存到data
        Article article =  new Article();
        
        //数据校验
        //检测userId
        if(userId == null || ("".equals(userId.trim()))){//用户未登录
        	statusMsg = "请登录！";
	        statusCode = 201;
	        data = statusMsg;
	        return webResponse.getWebResponse(statusCode, statusMsg, data);//返回WebResponse对象
        }
        else{//若userId不为空
            if (!userId.matches("^[0-9]*$")) {//用户参数错误
                statusMsg = " userId参数数字型错误！！！";
                statusCode = 201;
                return webResponse.getWebResponse(statusCode, statusMsg, data);
                }
            //检测用户小黑屋
            else{
            	Integer userIdNumeri = Integer.parseInt(userId);
            	Boolean userState=this.userUtils.getUserStateById(userIdNumeri);
	            if(!userState){
	            	statusMsg = "您已进入黑名单，无法发表文章";
	                statusCode = 201;
	                data = statusMsg;
	                return webResponse.getWebResponse(statusCode, statusMsg, data);//返回WebResponse对象
	                }
	            else{
	            	article.setUserId(userIdNumeri);
	            	}
	            }
            }     
        
        //检测标题格式
        if (title != null && !("".equals(title.trim()))) {
            if(title.length() > 50) {
                statusMsg = " 参数长度过长错误,title";
                statusCode = 201;
                return webResponse.getWebResponse(statusCode, statusMsg, data);
            }
            article.setTitle(title);
        }
        //检测内容格式
        if (content != null && !("".equals(content.trim()))) {
            if(content.length() > 65535) {
                statusMsg = " 参数长度过长错误,content";
                statusCode = 201;
                return webResponse.getWebResponse(statusCode, statusMsg, data);
            }
            //过滤文章内容
            String cont=sensitivewordFilterUtils.replaceSensitiveWord(content,1,"*");
            article.setContent(cont);
        }
        //检测tagsId格式
        if (!tagsId.matches("^[0-9]*$")) {//标签参数错误
            statusMsg = " tagsId参数数字型错误！！！";
            statusCode = 201;
            return webResponse.getWebResponse(statusCode, statusMsg, data);
            }
        //检测图片预览URL格式
        if(articlePhotoUrl.length() > 65535) {
            statusMsg = " 参数长度过长错误,articlePhotoUrl";
            statusCode = 201;
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }     
        
        //检测通过
        article.setTypeName(typeName);//设置是新闻or博客or论坛
        article.setClicks(0);//默认0点击量
        article.setIsPass("通过");//新闻默认通过
        article.setTbStatus("正常");//默认文章状态正常
        String preCont=sensitivewordFilterUtils.replaceSensitiveWord(preContent,1,"*");
        article.setPreContent(preCont);//生成预览
        this.articleService.insert(article);
        //若文章表插入成功
        if (article.getArticleId() > 0) {
            //插入文章-标签表
        	String tbStatus="正常";
        	ArticleTags articleTagsEntity=this.articleTagsUtils.setEntity(article.getArticleId(), tagsId, tbStatus);
            this.articleTagsService.insert(articleTagsEntity);
            //插入文章-预览图表
            ArticlePhoto articlePhotoEntity=this.articlePhotoUtils.setEntity(article.getArticleId(), articlePhotoUrl, tbStatus);
            this.articlePhotoService.insert(articlePhotoEntity);
            } else {
                statusCode = 202;
                statusMsg = "insert文章标签/图像预览失败";
            }
        return webResponse.getWebResponse(statusCode, statusMsg, data);

    }
    
    //获取博客列表
    @RequestMapping(value = "/getBlogArticleList", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse getBlogArticleList(HttpServletRequest request, HttpServletResponse response, HttpSession session,
            @RequestParam(defaultValue = "1", required = false) Integer pageNo,
            @RequestParam(defaultValue = "10", required = false) Integer pageSize,
            @RequestParam(defaultValue = "正常", required = false) String tbStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "article_id", required = false) String order,
            @RequestParam(defaultValue = "desc", required = false) String desc ){
        return this.getArticleTypeList(request, response, session,"博客",pageNo,pageSize,tbStatus,keyword,order,desc);
    }

    //获取文章类型列表(HttpServletRequest request, HttpServletResponse response, HttpSession session,typeName,pageNo,pageSize,tbStatus,keyword,order,desc)
    private WebResponse getArticleTypeList(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                                           @RequestParam String typeName,
                                           @RequestParam(defaultValue = "1", required = false) Integer pageNo,
                                           @RequestParam(defaultValue = "10", required = false) Integer pageSize,
                                           @RequestParam(defaultValue = "正常", required = false) String tbStatus,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String order,
                                           @RequestParam(defaultValue = "desc", required = false) String desc ) {
        Object data = null;
        int statusCode=200;
        String statusMsg="";
        LinkedHashMap<String, String> condition = new LinkedHashMap<String, String>();

        if (tbStatus != null && tbStatus.length() > 0) {
            condition.put("tb_status='" + tbStatus + "'", "and");
        }
        condition.put("type_name='"+typeName+"'", "and");//要新闻Or博客Or论坛
        condition.put("is_pass= '通过'","and");//要通过审核
        condition.put("top= '正常'","and");//要通过审核

        if (keyword != null && keyword.length() > 0) {//若搜索关键字不为空
            StringBuffer buf = new StringBuffer();
            buf.append("(");
            buf.append("title like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("content like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("type_name like '%").append(keyword).append("%'");
            buf.append(")");
            condition.put(buf.toString(), "and ");
        }
        String field = null;
        if (condition.size() > 0) {
            condition.put(condition.entrySet().iterator().next().getKey(), "");
        }
        int count = this.articleService.getCount(condition, field);//返回符合条件的个数
        order = "create_time desc";

        //ArticleVo列表
        List<ArticleVo> list = this.articleService.getList(condition, pageNo, pageSize, order, field);

        Map<Object, Object> map = new HashMap<Object, Object>();//data
        map.put("total", count);//新闻数量
        int size = list.size();
        JSONArray jsonArray=new JSONArray();
        if (size > 0) {//列表不为空，遍历列表拿到ArticleVo对象中的articleId,userId List
            for(ArticleVo articleVo:list){

                JSONObject json=new JSONObject();
                //获得用户昵称
                String userNickname=this.userUtils.getUserById(articleVo.getUserId()).getNickname();//userId
                json.put("userNickname", userNickname);
//                Article article=new Article();
//                BeanUtils.copyProperties(articlevo, article);
                json.put("article",articleVo);

                //获得文章标签名字
                Integer articleId =articleVo.getArticleId();//articleId
                LinkedHashMap<String, String> articleTagscondition = new LinkedHashMap<String, String>();
                articleTagscondition.put("article_id='"+articleId+"'", "and");
                //获得文章标签中间表
                List<ArticleTagsVo> articleTagsList=this.articleTagsService.getList(articleTagscondition, pageNo, pageSize);
                //获得标签列表中的tags_name By tags_id
                List<Tags> tagsList=new LinkedList<Tags>();
                //遍历中间表对象
                for(ArticleTagsVo articleTagsvo:articleTagsList){
                    Integer tagsId=articleTagsvo.getTagsId();//tags_id
                    TagsVo tagsvo=this.tagsService.getById(tagsId);
                    if(tagsvo!=null){
                        Tags tags=new Tags();
                        BeanUtils.copyProperties(tagsvo,tags);
                        tagsList.add(tags);}
                }
                json.put("tagsList", tagsList);

                //获得预览图像的链接
                LinkedHashMap<String, String> articlePhotocondition = new LinkedHashMap<String, String>();
                articlePhotocondition.put("article_id='"+articleId+"'", "and");
                //获得文章图片列表
                ArticlePhotoVo articlePhotoVo=this.articlePhotoService.getOne(articlePhotocondition);
                if(articlePhotoVo!=null){
                    String articlePhotoURL=articlePhotoVo.getArticlePhotoUrl();
                    json.put("articlePhotoUrl", articlePhotoURL);
                }
                jsonArray.add(json);
            }
            map.put("list", jsonArray);

            data = map;
            statusMsg = "根据条件获取分页数据成功！！！";
        } else {
            map.put("list", list);
            data = map;
            statusCode = 202;
            statusMsg = "no record!!!";
        }
        return webResponse.getWebResponse(statusCode, statusMsg, data);
    }

    //获取博客置顶列表
    @RequestMapping(value = "/getTopArticleList", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse getTopArticleList(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                                          @RequestParam(defaultValue = "1", required = false) Integer pageNo,
                                          @RequestParam(defaultValue = "10", required = false) Integer pageSize,
                                          @RequestParam(defaultValue = "正常", required = false) String tbStatus,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "article_id", required = false) String order,
                                          @RequestParam(defaultValue = "desc", required = false) String desc ){
        return this.getTopArticleTypeList(request, response, session,"博客",pageNo,pageSize,tbStatus,keyword,order,desc);
    }

    //获取置顶文章列表(HttpServletRequest request, HttpServletResponse response, HttpSession session,typeName,pageNo,pageSize,tbStatus,keyword,order,desc)
    private WebResponse getTopArticleTypeList(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                                           @RequestParam String typeName,
                                           @RequestParam(defaultValue = "1", required = false) Integer pageNo,
                                           @RequestParam(defaultValue = "10", required = false) Integer pageSize,
                                           @RequestParam(defaultValue = "正常", required = false) String tbStatus,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String order,
                                           @RequestParam(defaultValue = "desc", required = false) String desc ) {
        Object data = null;
        int statusCode=200;
        String statusMsg="";
        LinkedHashMap<String, String> condition = new LinkedHashMap<String, String>();

        if (tbStatus != null && tbStatus.length() > 0) {
            condition.put("tb_status='" + tbStatus + "'", "and");
        }
        condition.put("type_name='"+typeName+"'", "and");//要新闻Or博客Or论坛
        condition.put("is_pass= '通过'","and");//要通过审核
        condition.put("top= '置顶'","and");//要通过审核

        if (keyword != null && keyword.length() > 0) {//若搜索关键字不为空
            StringBuffer buf = new StringBuffer();
            buf.append("(");
            buf.append("title like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("content like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("type_name like '%").append(keyword).append("%'");
            buf.append(")");
            condition.put(buf.toString(), "and ");
        }
        String field = null;
        if (condition.size() > 0) {
            condition.put(condition.entrySet().iterator().next().getKey(), "");
        }
        int count = this.articleService.getCount(condition, field);//返回符合条件的个数
        //按照 置顶-->置顶时间-->创建时间 来排序
        order = "top='置顶' desc,top_time desc,create_time desc";

        //ArticleVo列表
        List<ArticleVo> list = this.articleService.getList(condition, pageNo, pageSize, order, field);

        Map<Object, Object> map = new HashMap<Object, Object>();//data
        map.put("total", count);//新闻数量
        int size = list.size();
        JSONArray jsonArray=new JSONArray();
        if (size > 0) {//列表不为空，遍历列表拿到ArticleVo对象中的articleId,userId List
            for(ArticleVo articlevo:list){

                JSONObject json=new JSONObject();
                //获得用户昵称
                String userNickname=this.userUtils.getUserById(articlevo.getUserId()).getNickname();//userId
                json.put("userNickname", userNickname);
                Article article=new Article();
                BeanUtils.copyProperties(articlevo, article);
                json.put("article",article);

                //获得文章标签名字
                Integer articleId =articlevo.getArticleId();//articleId
                LinkedHashMap<String, String> articleTagscondition = new LinkedHashMap<String, String>();
                articleTagscondition.put("article_id='"+articleId+"'", "and");
                //获得文章标签中间表
                List<ArticleTagsVo> articleTagsList=this.articleTagsService.getList(articleTagscondition, pageNo, pageSize);
                //获得标签列表中的tags_name By tags_id
                List<Tags> tagsList=new LinkedList<Tags>();
                //遍历中间表对象
                for(ArticleTagsVo articleTagsvo:articleTagsList){
                    Integer tagsId=articleTagsvo.getTagsId();//tags_id
                    TagsVo tagsvo=this.tagsService.getById(tagsId);
                    if(tagsvo!=null){
                        Tags tags=new Tags();
                        BeanUtils.copyProperties(tagsvo,tags);
                        tagsList.add(tags);}
                }
                json.put("tagsList", tagsList);

                //获得预览图像的链接
                LinkedHashMap<String, String> articlePhotocondition = new LinkedHashMap<String, String>();
                articlePhotocondition.put("article_id='"+articleId+"'", "and");
                //获得文章图片列表
                ArticlePhotoVo articlePhotoVo=this.articlePhotoService.getOne(articlePhotocondition);
                if(articlePhotoVo!=null){
                    String articlePhotoURL=articlePhotoVo.getArticlePhotoUrl();
                    json.put("articlePhotoUrl", articlePhotoURL);
                }
                jsonArray.add(json);
            }
            map.put("list", jsonArray);

            data = map;
            statusMsg = "根据条件获取分页数据成功！！！";
        } else {
            map.put("list", list);
            data = map;
            statusCode = 202;
            statusMsg = "no record!!!";
        }
        return webResponse.getWebResponse(statusCode, statusMsg, data);
    }
    
    
    //获取未审核的文章列表(管理员功能）
    @RequestMapping(value = "/examineArticleList", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String examineArticleList(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                                      @RequestParam(defaultValue = "1", required = false) Integer pageNo,
                                      @RequestParam(defaultValue = "10", required = false) Integer pageSize,
                                      @RequestParam(defaultValue = "正常", required = false) String tbStatus,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "article_id", required = false) String order,
                                      @RequestParam(defaultValue = "desc", required = false) String desc ) {
        Object data = null;
        String statusMsg = "";
        int statusCode = 200;
        LinkedHashMap<String, String> condition = new LinkedHashMap<String, String>();

        if (tbStatus != null && tbStatus.length() > 0) {
            condition.put("tb_status='" + tbStatus + "'", "and");
        }
        condition.put("is_pass= '未审核'","and");
        if (keyword != null && keyword.length() > 0) {
            StringBuffer buf = new StringBuffer();
            buf.append("(");
            buf.append("title like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("content like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("pre_content like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("type_name like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("is_pass like '%").append(keyword).append("%'");
            buf.append(")");
            condition.put(buf.toString(), "and");
        }
        String field = null;
        if (condition.size() > 0) {
            condition.put(condition.entrySet().iterator().next().getKey(), "");
        }
        int count = this.articleService.getCount(condition, field);
        if (order != null && order.length() > 0 & "desc".equals(desc)) {
            order = order + " desc";
        }
        List<ArticleVo> list = this.articleService.getList(condition, pageNo, pageSize, order, field);
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("total", count);
        int size = list.size();
        if (size > 0) {
            map.put("list", list);
            data = map;
            statusMsg = "根据条件获取分页数据成功！！！";
        } else {
            map.put("list", list);
            data = map;
            statusCode = 202;
            statusMsg = "no record!!!";
        }
        return JSON.toJSONString(data);
    }
    
    //根据标签获取文章列表
    /*
    @RequestMapping(value = "/getTagArticleList", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getTagArticleList(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                                    @RequestParam(defaultValue = "1", required = false) Integer pageNo,
                                    @RequestParam(defaultValue = "10", required = false) Integer pageSize,
                                    @RequestParam(defaultValue = "正常", required = false) String tbStatus,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Integer tagId,
                                    @RequestParam(defaultValue = "article_id", required = false) String order,
                                    @RequestParam(defaultValue = "desc", required = false) String desc ) {
        Object data = null;
        String statusMsg = "";
        int statusCode = 200;
        LinkedHashMap<String, String> condition = new LinkedHashMap<String, String>();
        UserCookie userCookie = this.userUtils.getLoginUser(request, response, session);

        if (tbStatus != null && tbStatus.length() > 0) {
            condition.put("tb_status='" + tbStatus + "'", "and");
        }
        condition.put("is_pass= '通过'","and");//要通过审核
        //tag文章列表
        List<Map<String,Object>> taglist=this.articleService.getArticlesByTagId(tagId);
        if (keyword != null && keyword.length() > 0) {
            StringBuffer buf = new StringBuffer();
            buf.append("(");
            buf.append("title like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("content like '%").append(keyword).append("%'");
            buf.append(" or ");
            buf.append("type_name like '%").append(keyword).append("%'");
            buf.append(")");
            condition.put(buf.toString(), "and ");
        }
        String field = null;
        if (condition.size() > 0) {
            condition.put(condition.entrySet().iterator().next().getKey(), "");
        }
        int count = this.articleService.getCount(condition, field);
        if (order != null && order.length() > 0 & "desc".equals(desc)) {
            order = order + " desc";
        }
        List<ArticleVo> list = this.articleService.getList(condition, pageNo, pageSize, order, field);
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("total", count);
        map.put("list", taglist);//tag文章列表
        int size = list.size();
        if (size > 0) {
            map.put("list", list);
            data = map;
            statusMsg = "根据条件获取分页数据成功！！！";
        } else {
            map.put("list", list);
            data = map;
            statusCode = 202;
            statusMsg = "no record!!!";
        }
        return JSON.toJSONString(data);
    }
    */

    //插入图片
    @RequestMapping(value = "/insertPic", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public JSONObject insertPic(HttpServletRequest request){
        Map<String,Object> map=new HashedMap();
        JSONArray jsonArray=new JSONArray();
        JSONObject jsonObject=new JSONObject();
        MultipartRequest multipartRequest= (MultipartRequest) request;
        Iterator<String> iterator=multipartRequest.getFileNames();
        MultipartFile multipartFile=null;
        while (iterator.hasNext()){
            String str = iterator.next();    //这个文件并不是原来的文件名
            multipartFile = (MultipartFile) multipartRequest.getFile(str);
            multipartFile.getOriginalFilename();
            String fileName= UUID.randomUUID().toString()+multipartFile.getOriginalFilename();
            String url="";
            //进行cos对象上传操作
            OSSUtils utils=new OSSUtils();
            OSSClient ossClient=utils.createCilent();
            try {
                url=utils.upload(fileName,multipartFile.getInputStream(),ossClient);//图片的url
                jsonArray.add(url);
                jsonObject.put("data",url);
            } catch (IOException e) {
                e.printStackTrace();
                jsonObject.put("errno",1);
                jsonObject.put("data","上传失败！");
            }
        }
        jsonObject.put("errno",0);
        return jsonObject;
    }
  
    //发表博客
    @RequestMapping(value = "/publishBlogArticle", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse publishBlogArticle(HttpServletRequest request, HttpServletResponse response, HttpSession session, String userId, String title, String content,  String tagsId,String articlePhotoUrl) {
        return this.newArticle(request, response, session, userId, title, content, tagsId,articlePhotoUrl, "博客");
    }
    
    

    //审核文章（管理员审核未审核文章）
    @RequestMapping(value = "/examineArticle", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse examineArticle(HttpServletRequest request, HttpServletResponse response, HttpSession session, String articleId,String isPass) {
        Object data = null;
        String statusMsg = "";
        Integer statusCode = 200;
        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("articleId", articleId);
        data = paramMap;
        if (articleId == null || "".equals(articleId.trim())) {
            statusMsg = "未获得主键参数错误！！！";
            statusCode = 201;
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }
        Integer articleIdNumeri = articleId.matches("^[0-9]*$") ? Integer.parseInt(articleId) : 0;
        if (articleIdNumeri == 0) {
            statusMsg = "主键不为数字错误！！！";
            statusCode = 201;
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }
        //获得原文章
        ArticleVo articleVo = this.articleService.getById(articleIdNumeri);
        Article article = new Article();
        BeanUtils.copyProperties(articleVo, article);  
    
        if (isPass != null && !("".equals(isPass.trim()))) {
            if(isPass.length() > 50) {
                statusMsg = " 参数长度过长错误,isPass";
                statusCode = 201;
                return webResponse.getWebResponse(statusCode, statusMsg, data);
            }
            article.setIsPass(isPass);
        }

        int num = this.articleService.update(article);
        if (num > 0) {
            statusMsg = "审核通过！！！";
        } else {
            statusCode = 202;
            statusMsg = "审核错误";
        }
        return webResponse.getWebResponse(statusCode, statusMsg, data);
    }

    //删除文章（不删数据库记录
    @RequestMapping(value = "/deleteArticle", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse deleteArticle(HttpServletRequest request, HttpServletResponse response, HttpSession session, String articleId) {
        Object data = null;
        String statusMsg = "";
        Integer statusCode = 200;
        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("articleId", articleId);
        data = paramMap;
        if (articleId == null || "".equals(articleId.trim())) {
            statusMsg = "未获得主键参数错误！！！";
            statusCode = 201;
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }
        Integer articleIdNumeri = articleId.matches("^[0-9]*$") ? Integer.parseInt(articleId) : 0;
        if (articleIdNumeri == 0) {
            statusMsg = "主键不为数字错误！！！";
            statusCode = 201;
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }
        //获得原文章
        ArticleVo articleVo = this.articleService.getById(articleIdNumeri);
        Article article = new Article();
        BeanUtils.copyProperties(articleVo, article);  
    
        article.setTbStatus("删除");
        int num = this.articleService.update(article);
        if (num > 0) {
            statusMsg = "文章已删除！！！";
        } else {
            statusCode = 202;
            statusMsg = "删除错误";
        }
        return webResponse.getWebResponse(statusCode, statusMsg, data);
    }

    //置顶文章
    @RequestMapping(value = "/topArticle", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse topArticle(HttpServletRequest request, HttpServletResponse response, HttpSession session, String articleId) {
        Object data = null;
        String statusMsg = "";
        Integer statusCode = 200;
        JSONObject jsonObject=new JSONObject();
        if (articleId == null || "".equals(articleId.trim())) {
            statusMsg = "未获得主键参数错误！！！";
            statusCode = 201;
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }

        //获得原文章
        try{
            ArticleVo articleVo = this.articleService.getById(Integer.parseInt(articleId));
            articleVo.setTop("置顶");
            Article article=new Article();
            BeanUtils.copyProperties(articleVo,article);
            this.articleService.update(article);
            statusMsg="操作成功！";
        }catch (Exception e){
            statusMsg="操作失败！";
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }

        return webResponse.getWebResponse(statusCode, statusMsg, data);
    }

    //取消置顶文章
    @RequestMapping(value = "/disTopArticle", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public WebResponse disTopArticle(HttpServletRequest request, HttpServletResponse response, HttpSession session, String articleId) {
        Object data = null;
        String statusMsg = "";
        Integer statusCode = 200;

        if (articleId == null || "".equals(articleId.trim())) {
            statusMsg = "未获得主键参数错误！！！";
            statusCode = 201;
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }

        //获得原文章
        try{
            ArticleVo articleVo = this.articleService.getById(Integer.parseInt(articleId));
            articleVo.setTop("正常");
            Article article=new Article();
            BeanUtils.copyProperties(articleVo,article);
            this.articleService.update(article);
            statusMsg="操作成功！";
        }catch (Exception e){
            statusMsg="操作失败！";
            return webResponse.getWebResponse(statusCode, statusMsg, data);
        }

        return webResponse.getWebResponse(statusCode, statusMsg, data);
    }



}

