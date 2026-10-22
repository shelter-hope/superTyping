// 首页
import { Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import EvolutionDemo from '@/components/home/EvolutionDemo';
import { Zap, Target, TrendingUp, Trophy } from 'lucide-react';

export default function HomePage() {
  const { user } = useAuth();

  return (
    <div className="container py-8 xl:py-16">
      {/* 英雄区 */}
      <section className="text-center space-y-6 py-12 xl:py-20">
        <h1 className="text-4xl xl:text-6xl font-bold gradient-text">
          打字进化游戏
        </h1>
        <p className="mx-auto max-w-2xl text-lg text-muted-foreground xl:text-xl">
          通过智能算法分析你的打字习惯，让字符怪兽自适应进化，<br />
          迫使你跳出舒适区，全面提升打字水平
        </p>
        <EvolutionDemo />
        <div className="flex flex-col sm:flex-row gap-4 justify-center pt-4">
          {user ? (
            <Link to="/prepare">
              <Button size="lg" className="w-full sm:w-auto">
                <Zap className="mr-2 h-5 w-5" />
                开始游戏
              </Button>
            </Link>
          ) : (
            <>
              <Link to="/register">
                <Button size="lg" className="w-full sm:w-auto">
                  立即注册
                </Button>
              </Link>
              <Link to="/login">
                <Button size="lg" variant="outline" className="w-full sm:w-auto">
                  登录
                </Button>
              </Link>
            </>
          )}
          <Link to="/leaderboard">
            <Button size="lg" variant="outline" className="w-full sm:w-auto">
              <Trophy className="mr-2 h-5 w-5" />
              排行榜
            </Button>
          </Link>
        </div>
      </section>

      {/* 特色功能 */}
      <section className="py-12 xl:py-16">
        <h2 className="text-3xl font-bold text-center mb-12">核心特色</h2>
        <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
          <Card>
            <CardHeader>
              <Target className="w-10 h-10 text-primary mb-2" />
              <CardTitle>自适应难度</CardTitle>
              <CardDescription>
                实时分析你的打字准确率和速度，动态调整怪物数量、字符长度和下落速度
              </CardDescription>
            </CardHeader>
          </Card>

          <Card>
            <CardHeader>
              <TrendingUp className="w-10 h-10 text-secondary mb-2" />
              <CardTitle>遗传算法进化</CardTitle>
              <CardDescription>
                字符怪兽会根据你的弱点进化，优先生成你容易出错的字符组合
              </CardDescription>
            </CardHeader>
          </Card>

          <Card>
            <CardHeader>
              <Trophy className="w-10 h-10 text-accent mb-2" />
              <CardTitle>数据分析</CardTitle>
              <CardDescription>
                详细记录每个字符的准确率和错误次数，生成个性化训练建议
              </CardDescription>
            </CardHeader>
          </Card>
        </div>
      </section>

      {/* 游戏模式 */}
      <section className="py-12 xl:py-16">
        <h2 className="text-3xl font-bold text-center mb-12">游戏模式</h2>
        <div className="grid gap-6 md:grid-cols-2 max-w-4xl mx-auto">
          <Card>
            <CardHeader>
              <CardTitle>无尽适应模式</CardTitle>
              <CardDescription className="space-y-2">
                <p>系统自动分析你的打字数据，生成针对性的字符组合</p>
                <ul className="list-disc list-inside space-y-1 text-sm">
                  <li>自动识别易错字符</li>
                  <li>动态调整难度</li>
                  <li>无限轮次挑战</li>
                </ul>
              </CardDescription>
            </CardHeader>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>词源界定模式</CardTitle>
              <CardDescription className="space-y-2">
                <p>上传自定义词库，在特定词汇范围内进行训练</p>
                <ul className="list-disc list-inside space-y-1 text-sm">
                  <li>支持自定义词库</li>
                  <li>专业术语训练</li>
                  <li>编程语言练习</li>
                </ul>
              </CardDescription>
            </CardHeader>
          </Card>
        </div>
      </section>

      {/* 怪物类型 */}
      <section className="py-12 xl:py-16">
        <h2 className="text-3xl font-bold text-center mb-12">字符怪物类型</h2>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4 max-w-6xl mx-auto">
          <Card>
            <CardContent className="pt-6">
              <div className="text-center space-y-2">
                <div className="text-3xl font-bold text-primary">普通型</div>
                <p className="text-sm text-muted-foreground">匀速前进，1积分</p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6">
              <div className="text-center space-y-2">
                <div className="text-3xl font-bold text-accent">攻击型</div>
                <p className="text-sm text-muted-foreground">定期生成新怪物，3积分</p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6">
              <div className="text-center space-y-2">
                <div className="text-3xl font-bold text-accent">回避型</div>
                <p className="text-sm text-muted-foreground">30%概率规避攻击，2积分</p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6">
              <div className="text-center space-y-2">
                <div className="text-3xl font-bold text-[hsl(var(--amber))]">智慧型</div>
                <p className="text-sm text-muted-foreground">吸引周围怪物，3积分</p>
              </div>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* 操作说明 */}
      <section className="py-12 xl:py-16 bg-muted/30 rounded-lg">
        <div className="container">
          <h2 className="text-3xl font-bold text-center mb-8">操作说明</h2>
          <div className="max-w-2xl mx-auto space-y-4 text-muted-foreground">
            <div className="flex gap-4">
              <span className="font-bold text-foreground min-w-24">键盘输入</span>
              <span>敲击对应字符攻击怪物，首次输入自动锁定目标</span>
            </div>
            <div className="flex gap-4">
              <span className="font-bold text-foreground min-w-24">Tab键</span>
              <span>取消当前锁定，重新选择目标</span>
            </div>
            <div className="flex gap-4">
              <span className="font-bold text-foreground min-w-24">Shift键</span>
              <span>切换中英文输入模式</span>
            </div>
            <div className="flex gap-4">
              <span className="font-bold text-foreground min-w-24">Caps Lock</span>
              <span>切换大小写</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
