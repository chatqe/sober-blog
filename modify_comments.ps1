# 获取当前时间，格式为 yyyy-MM-dd HH:mm
$currentTime = Get-Date -Format "yyyy-MM-dd HH:mm"

# 读取所有需要修改的文件路径
$files = @(
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\behaviorState\service\BehaviorCntServiceImpl.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\behaviorState\service\BehaviorCntService.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\behaviorState\domain\dao\BehaviorCntMapper.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\behaviorState\domain\pojo\BehaviorCnt.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\behaviorState\service\BehaviorServiceImpl.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\behaviorState\service\BehaviorService.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\behaviorState\domain\dao\BehaviorMapper.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\behaviorState\domain\pojo\Behavior.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\category\domain\dao\CategoryMapper.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\category\service\CategoryServiceImpl.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\category\service\CategoryService.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\user\service\UserServiceImpl.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\moment\service\MomentServiceImpl.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\moment\service\MomentService.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\moment\domain\dao\MomentMapper.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\moment\domain\pojo\Moment.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\danmaku\controller\web\DanmakuController.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\danmaku\domain\dao\DanmakuMapper.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\danmaku\domain\pojo\Danmaku.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\tag\service\TagServiceImpl.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\tag\service\TagService.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\tag\domain\dao\TagMapper.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\tag\domain\pojo\Tag.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\sysdict\service\SysDictTypeServiceImpl.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\sysdict\service\SysDictTypeService.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\modules\sysdict\domain\dao\SysDictTypeMapper.java",
    "e:\idea\qs-blog\qsblog-server\src\main\java\io\charqe\qs\