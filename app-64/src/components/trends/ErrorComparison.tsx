// 字符错误次数对比组件
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

interface ErrorComparisonProps {
  thisWeekErrors: number;
  lastWeekErrors: number;
}

export default function ErrorComparison({ thisWeekErrors, lastWeekErrors }: ErrorComparisonProps) {
  const difference = thisWeekErrors - lastWeekErrors;
  const percentageChange = lastWeekErrors > 0 
    ? ((difference / lastWeekErrors) * 100).toFixed(1)
    : '0';
  
  const isImprovement = difference < 0;
  const isNoChange = difference === 0;

  return (
    <Card>
      <CardHeader>
        <CardTitle>字符错误次数对比</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <p className="text-sm text-muted-foreground">本周错误次数</p>
              <p className="text-3xl font-bold text-primary">{thisWeekErrors}</p>
            </div>
            <div className="space-y-1">
              <p className="text-sm text-muted-foreground">上周错误次数</p>
              <p className="text-3xl font-bold text-muted-foreground">{lastWeekErrors}</p>
            </div>
          </div>

          <div className="pt-4 border-t">
            <div className="flex items-center gap-2">
              {isNoChange ? (
                <>
                  <Minus className="w-5 h-5 text-muted-foreground" />
                  <span className="text-lg font-medium text-muted-foreground">
                    与上周持平
                  </span>
                </>
              ) : isImprovement ? (
                <>
                  <TrendingDown className="w-5 h-5 text-green-500" />
                  <span className="text-lg font-medium text-green-500">
                    减少 {Math.abs(difference)} 次 ({percentageChange}%)
                  </span>
                </>
              ) : (
                <>
                  <TrendingUp className="w-5 h-5 text-red-500" />
                  <span className="text-lg font-medium text-red-500">
                    增加 {difference} 次 (+{percentageChange}%)
                  </span>
                </>
              )}
            </div>
            <p className="mt-2 text-sm text-muted-foreground">
              {isImprovement 
                ? '🎉 太棒了！你的打字准确率在提升！' 
                : isNoChange
                ? '保持稳定，继续加油！'
                : '💪 继续练习，你会越来越好的！'}
            </p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
