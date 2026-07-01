// 结算页面
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { createGameRecord, updateCharacterStat } from '@/db/api';
import { useToast } from '@/hooks/use-toast';
import { Trophy, TrendingUp, Target, RotateCcw } from 'lucide-react';

export default function ResultPage() {
  const [result, setResult] = useState<any>(null);
  const [saving, setSaving] = useState(false);
  const { user } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();

  useEffect(() => {
    const savedResult = sessionStorage.getItem('gameResult');
    if (savedResult) {
      const data = JSON.parse(savedResult);
      setResult(data);
      saveGameData(data);
    } else {
      navigate('/');
    }
  }, []);

  async function saveGameData(data: any) {
    if (!user || saving) return;
    
    setSaving(true);
    try {
      const { stats, characterStats } = data;
      const gameDuration = Math.floor((Date.now() - stats.startTime) / 1000);

      await createGameRecord({
        gameMode: 'endless',
        totalScore: stats.score,
        roundNum: stats.rounds,
        avgSpeed: stats.currentSpeed,
        peakSpeed: stats.peakSpeed,
        avgAccuracy: stats.currentAccuracy,
        peakAccuracy: stats.peakAccuracy,
        totalChars: stats.totalCharacters,
        correctChars: stats.correctCharacters,
        gameDuration: gameDuration,
      });

      for (const [char, stat] of characterStats) {
        const isCorrect = stat.accuracy > 50;
        await updateCharacterStat(user.userId, char, isCorrect);
      }

      toast({
        title: '数据已保存',
        description: '游戏记录已成功保存',
      });
    } catch (error) {
      console.error('保存游戏数据失败:', error);
    } finally {
      setSaving(false);
    }
  }

  if (!result) return null;

  const { stats } = result;

  return (
    <div className="container max-w-4xl py-8">
      <div className="space-y-6">
        <div className="text-center space-y-2">
          <h1 className="text-4xl font-bold gradient-text">游戏结束</h1>
          <p className="text-muted-foreground">查看你的表现数据</p>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Trophy className="w-5 h-5 text-primary" />
                总积分
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-4xl font-bold text-primary">{stats.score}</div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Target className="w-5 h-5 text-secondary" />
                游戏轮数
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-4xl font-bold text-secondary">{stats.rounds}</div>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-accent" />
              打字数据
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <div className="text-sm text-muted-foreground">平均速度</div>
                <div className="text-2xl font-bold">{stats.currentSpeed.toFixed(0)} 字符/分钟</div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">巅峰速度</div>
                <div className="text-2xl font-bold text-secondary">{stats.peakSpeed.toFixed(0)} 字符/分钟</div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">平均准确率</div>
                <div className="text-2xl font-bold">{stats.currentAccuracy.toFixed(1)}%</div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">巅峰准确率</div>
                <div className="text-2xl font-bold text-accent">{stats.peakAccuracy.toFixed(1)}%</div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">总字符数</div>
                <div className="text-2xl font-bold">{stats.totalCharacters}</div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">正确字符数</div>
                <div className="text-2xl font-bold text-secondary">{stats.correctCharacters}</div>
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Button size="lg" onClick={() => navigate('/prepare')}>
            <RotateCcw className="mr-2 h-5 w-5" />
            再玩一局
          </Button>
          <Button size="lg" variant="outline" onClick={() => navigate('/profile')}>
            查看个人数据
          </Button>
          <Button size="lg" variant="outline" onClick={() => navigate('/leaderboard')}>
            查看排行榜
          </Button>
        </div>
      </div>
    </div>
  );
}
